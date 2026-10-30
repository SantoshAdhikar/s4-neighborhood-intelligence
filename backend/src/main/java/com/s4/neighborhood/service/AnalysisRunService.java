package com.s4.neighborhood.service;

import com.s4.neighborhood.dto.AnalysisRunRequest;
import com.s4.neighborhood.dto.AnalysisRunResponse;
import com.s4.neighborhood.dto.AnalysisRunHistoryResponse;
import com.s4.neighborhood.entity.AnalysisRun;
import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.entity.WebsiteFinding;
import com.s4.neighborhood.repository.AnalysisRunRepository;
import com.s4.neighborhood.repository.BusinessProfileRepository;
import com.s4.neighborhood.repository.WebsiteFindingRepository;
import com.s4.neighborhood.repository.WebsitePageFindingRepository;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
public class AnalysisRunService {
    private final AnalysisRunRepository runs;
    private final WebsiteFindingRepository websites;
    private final WebsitePageFindingRepository pages;
    private final BusinessProfileRepository businesses;
    private final StorefrontMediaService media;
    private final NeighborhoodResearchService neighborhood;
    private final WebsiteResearchService websiteResearch;
    private final StorefrontSegmentationService segmentation;
    private final EvidenceReportService reports;
    private final Executor executor = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(4), new ThreadPoolExecutor.AbortPolicy());

    public AnalysisRunService(AnalysisRunRepository runs, WebsiteFindingRepository websites,
                              WebsitePageFindingRepository pages,
                              BusinessProfileRepository businesses, StorefrontMediaService media,
                              NeighborhoodResearchService neighborhood, WebsiteResearchService websiteResearch,
                              StorefrontSegmentationService segmentation, EvidenceReportService reports) {
        this.runs = runs;
        this.websites = websites;
        this.pages = pages;
        this.businesses = businesses;
        this.media = media;
        this.neighborhood = neighborhood;
        this.websiteResearch = websiteResearch;
        this.segmentation = segmentation;
        this.reports = reports;
    }

    public AnalysisRunResponse start(UUID businessId, AnalysisRunRequest request) {
        BusinessProfile business = businesses.findById(businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));
        if (runs.existsByBusinessIdAndStatusIn(businessId, List.of("QUEUED", "RUNNING"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An analysis is already running for this business");
        }
        String goal = request.goal() == null || request.goal().isBlank()
                ? business.getPrimaryGoal() : request.goal().strip();
        if (goal == null || goal.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add a business goal");
        }
        String url = request.website() == null || request.website().isBlank()
                ? business.getWebsite() : request.website().strip();
        if (url != null && !url.isBlank() && (!url.startsWith("https://") || url.length() > 500)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Website must begin with https:// for safe, free homepage research");
        }
        UUID mediaId = request.mediaId();
        if (mediaId != null) media.file(businessId, mediaId);
        if (mediaId == null) {
            mediaId = media.list(businessId).stream().filter(item -> "ANALYZED".equals(item.status()))
                    .map(item -> item.id()).findFirst().orElse(null);
        }
        AnalysisRun run = new AnalysisRun();
        run.setBusinessId(businessId);
        run.setGoal(goal);
        String focus = request.focusServices() == null || request.focusServices().isBlank()
                ? business.getServices() : request.focusServices().strip();
        run.setFocusServices(focus == null ? "" : focus.substring(0, Math.min(500, focus.length())));
        run.setWebsite(url);
        run.setCustomerFeedback(request.customerFeedback() == null ? "" : request.customerFeedback().strip());
        run.setMediaId(mediaId);
        run.setUseMeta(request.useMeta());
        run.setSegmentSign(request.useMeta() && request.segmentSign());
        run.setDetail("Analysis queued");
        try {
            run = runs.saveAndFlush(run);
        } catch (DataIntegrityViolationException conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An analysis is already running", conflict);
        }
        UUID runId = run.getId();
        try {
            executor.execute(() -> process(runId));
        } catch (RejectedExecutionException unavailable) {
            fail(runId, "Local analysis capacity is full. Try again after another run finishes.");
        }
        return response(runs.findById(runId).orElseThrow());
    }

    public Optional<AnalysisRunResponse> latest(UUID businessId) {
        return runs.findTopByBusinessIdOrderByCreatedAtDesc(businessId).map(this::response);
    }

    public AnalysisRunHistoryResponse history(UUID businessId, int page) {
        if (page < 0 || page > 10000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid history page");
        }
        var request = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var result = runs.findByBusinessId(businessId, request);
        var items = result.getContent().stream()
                .map(run -> new AnalysisRunHistoryResponse.Item(run.getId(), run.getStatus(),
                        run.getGoal(), run.getCreatedAt(), run.getCompletedAt(),
                        run.getReportId(), run.isUseMeta()))
                .toList();
        return new AnalysisRunHistoryResponse(items, page, result.hasNext());
    }

    public AnalysisRunResponse get(UUID businessId, UUID runId) {
        return runs.findByIdAndBusinessId(runId, businessId).map(this::response)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis run not found"));
    }

    void process(UUID id) {
        try {
            AnalysisRun run = runs.findById(id).orElseThrow();
            BusinessProfile business = businesses.findById(run.getBusinessId()).orElseThrow();
            stage(id, "NEIGHBORHOOD", "Finding and caching nearby businesses");
            business.setPrimaryGoal(run.getGoal());
            if (run.getWebsite() != null && !run.getWebsite().isBlank()) business.setWebsite(run.getWebsite());
            business = businesses.saveAndFlush(business);
            boolean usedSavedNeighborhood = false;
            boolean neighborhoodUnavailable = false;
            try {
                neighborhood.research(run.getBusinessId());
            } catch (ResponseStatusException providerError) {
                // Neighborhood data is one evidence source, not a prerequisite for
                // analyzing an owner's own website. Never substitute another address.
                if (!isRecoverableNeighborhoodFailure(providerError)) throw providerError;
                usedSavedNeighborhood = neighborhood.hasSavedSnapshot(run.getBusinessId());
                neighborhoodUnavailable = !usedSavedNeighborhood;
                stage(id, "NEIGHBORHOOD", usedSavedNeighborhood
                        ? "Using saved neighborhood evidence; live provider unavailable"
                        : "No neighborhood evidence available; continuing with the business website");
            }
            stage(id, "WEBSITES", "Reviewing publicly accessible business homepages");
            websiteResearch.research(run, business);
            if (run.isUseMeta()) {
                if (run.getMediaId() != null) {
                    stage(id, "VISUAL", "Reviewing selected storefront image with Meta if needed");
                    var result = media.analyze(run.getBusinessId(), run.getMediaId());
                    if (!"ANALYZED".equals(result.status())) {
                        throw new IllegalStateException(result.lastError() == null
                                ? "Storefront analysis did not complete" : result.lastError());
                    }
                    if (run.isSegmentSign() && result.mimeType().startsWith("image/") && result.sizeBytes() <= 8_388_608) {
                        stage(id, "SEGMENTATION", "Locating storefront sign with Meta SAM 3.1");
                        var masks = segmentation.segment(run.getBusinessId(), run.getMediaId(), "storefront sign");
                        if (!"COMPLETED".equals(masks.status())) {
                            throw new IllegalStateException("SAM segmentation failed; saved free research remains available");
                        }
                    }
                }
                stage(id, "REPORT", "Generating cited recommendations with Meta Muse Spark");
                var report = reports.generate(run.getBusinessId(), id);
                if (!"COMPLETED".equals(report.status())) {
                    throw new IllegalStateException(report.lastError() == null
                            ? "Opportunity report did not complete" : report.lastError());
                }
                run = runs.findById(id).orElseThrow();
                run.setReportId(report.id());
                runs.saveAndFlush(run);
            } else {
                stage(id, "REPORT", "Generating the action plan locally with Ollama; no API charges");
                var report = reports.generateLocal(run.getBusinessId(), id);
                if (!"COMPLETED".equals(report.status())) {
                    throw new IllegalStateException(report.lastError() == null
                            ? "Local report generation failed" : report.lastError());
                }
                run = runs.findById(id).orElseThrow();
                run.setReportId(report.id());
                runs.saveAndFlush(run);
            }
            run = runs.findById(id).orElseThrow();
            run.setStatus("COMPLETED");
            run.setStep("COMPLETE");
            String resultDetail = run.isUseMeta() ? "Research and Meta action report completed"
                    : "Free research and local Ollama action report completed. No Meta calls were made.";
            if (usedSavedNeighborhood) {
                resultDetail += " Earlier neighborhood results were reused because the live provider was unavailable.";
            }
            if (neighborhoodUnavailable) {
                resultDetail += " Neighborhood discovery was unavailable; this report does not establish local competitors.";
            }
            run.setDetail(resultDetail);
            run.setCompletedAt(Instant.now());
            runs.saveAndFlush(run);
        } catch (Exception exception) {
            String error = exception.getMessage() == null ? "Analysis could not finish" : exception.getMessage();
            fail(id, error);
        }
    }

    private static boolean isRecoverableNeighborhoodFailure(ResponseStatusException error) {
        int status = error.getStatusCode().value();
        return status == 422 || status == 429 || status == 502 || status == 503 || status == 504;
    }

    private void stage(UUID id, String step, String detail) {
        AnalysisRun run = runs.findById(id).orElseThrow();
        run.setStatus("RUNNING");
        run.setStep(step);
        run.setDetail(detail);
        runs.saveAndFlush(run);
    }

    private void fail(UUID id, String explanation) {
        runs.findById(id).ifPresent(run -> {
            run.setStatus("FAILED");
            run.setStep("ERROR");
            run.setLastError(explanation.substring(0, Math.min(explanation.length(), 500)));
            run.setDetail("Research interrupted; completed findings remain available");
            run.setCompletedAt(Instant.now());
            runs.saveAndFlush(run);
        });
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedRuns() {
        for (AnalysisRun run : runs.findByStatusIn(List.of("QUEUED", "RUNNING"))) {
            // Previous JVM cannot still own an in-flight run after this application's startup.
            fail(run.getId(), "The application restarted during analysis. You can start a new run.");
        }
    }

    private AnalysisRunResponse response(AnalysisRun run) {
        var findings = websites.findByRunIdOrderByFetchedAtAsc(run.getId()).stream()
                .map(f -> new AnalysisRunResponse.WebsiteRecord(f.getId(), f.getKind(), f.getBusinessName(), f.getUrl(),
                        f.getTitle(), f.getObservedSummary(), f.getStatus(), f.getNote(), f.getFetchedAt(),
                        f.getOsmSourceUrl(), f.getCategory(), f.getDistanceMiles(), f.getMatchLevel()))
                .toList();
        var details = pages.findByRunIdOrderByFetchedAtAsc(run.getId()).stream()
                .map(page -> new AnalysisRunResponse.PageRecord(page.getWebsiteFindingId(), page.getPageType(),
                        page.getPageUrl(), page.getPageTitle(), page.getStatus(),
                        page.getObservedSummary(), page.getNote(), page.getFetchedAt())).toList();
        return new AnalysisRunResponse(run.getId(), run.getStatus(), run.getStep(), run.getDetail(),
                run.getGoal(), run.getWebsite(), run.getFocusServices(), run.getMediaId(), run.getReportId(), run.isUseMeta(), run.isSegmentSign(),
                run.getLastError(), run.getCreatedAt(), run.getCompletedAt(), findings, details);
    }
}
