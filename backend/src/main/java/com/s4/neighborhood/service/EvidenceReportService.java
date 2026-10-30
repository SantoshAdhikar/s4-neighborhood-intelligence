package com.s4.neighborhood.service;

import com.s4.neighborhood.client.MetaOpportunityClient;
import com.s4.neighborhood.client.LocalOpportunityClient;
import com.s4.neighborhood.dto.EvidenceReportResponse;
import com.s4.neighborhood.dto.EvidenceReportResponse.*;
import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.entity.EvidenceReport;
import com.s4.neighborhood.repository.BusinessProfileRepository;
import com.s4.neighborhood.repository.EvidenceReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Service
public class EvidenceReportService {
    private static final Logger log = LoggerFactory.getLogger(EvidenceReportService.class);
    private final BusinessProfileRepository businesses;
    private final EvidenceReportRepository reports;
    private final EvidenceSourceService sources;
    private final MetaOpportunityClient meta;
    private final LocalOpportunityClient local;
    private final JsonMapper json;

    public EvidenceReportService(BusinessProfileRepository businesses, EvidenceReportRepository reports,
                                 EvidenceSourceService sources, MetaOpportunityClient meta, LocalOpportunityClient local, JsonMapper json) {
        this.businesses = businesses;
        this.reports = reports;
        this.sources = sources;
        this.meta = meta;
        this.local = local;
        this.json = json;
    }

    public Optional<EvidenceReportResponse> latest(UUID businessId) {
        return reports.findTopByBusinessIdOrderByCreatedAtDesc(businessId).map(this::toResponse);
    }

    public EvidenceReportResponse get(UUID businessId, UUID reportId) {
        return reports.findById(reportId)
                .filter(report -> businessId.equals(report.getBusinessId()))
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
    }

    public EvidenceReportResponse generate(UUID businessId) {
        return generate(businessId, false, null);
    }

    public EvidenceReportResponse generate(UUID businessId, UUID analysisRunId) {
        return generate(businessId, false, analysisRunId);
    }

    public EvidenceReportResponse generateLocal(UUID businessId) {
        return generate(businessId, true, null);
    }

    public EvidenceReportResponse generateLocal(UUID businessId, UUID analysisRunId) {
        return generate(businessId, true, analysisRunId);
    }

    private EvidenceReportResponse generate(UUID businessId, boolean localMode, UUID analysisRunId) {
        BusinessProfile business = businesses.findById(businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));
        EvidenceSourceService.EvidenceBundle bundle = sources.collect(business, false, analysisRunId);
        String fingerprint = fingerprint(bundle, localMode);
        EvidenceReport report = reports.findByBusinessIdAndSourceFingerprint(businessId, fingerprint)
                .orElseGet(() -> createReport(businessId, fingerprint));
        if ("COMPLETED".equals(report.getStatus())) return toResponse(report);
        int claimed = localMode ? reports.claimLocal(report.getId()) : reports.claim(report.getId());
        if (claimed != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    localMode ? "This local report is already generating"
                            : "This Meta report is running or has reached its retry limit");
        }
        report = reports.findById(report.getId()).orElseThrow();
        try {
            Draft draft;
            if (localMode) {
                LocalOpportunityClient.Result result = local.generate(bundle);
                draft = result.draft();
                try {
                    validateDraft(draft, bundle.sources());
                } catch (IllegalStateException incomplete) {
                    // One bounded local retry costs no external API credits.
                    result = local.generate(bundle, true);
                    draft = result.draft();
                }
                report.setModelName(result.model());
                report.setModelResponseId(null);
            } else {
                MetaOpportunityClient.Result result = meta.generate(bundle);
                draft = result.draft();
                report.setModelName(result.model());
                report.setModelResponseId(result.responseId());
            }
            validateDraft(draft, bundle.sources());
            SavedReport saved = new SavedReport(bundle.sources(), draft.opportunities(),
                    draft.plan(), mergeLimitations(bundle.limitations(), draft.limitations()));
            report.setReportJson(json.writeValueAsString(saved));
            report.setStatus("COMPLETED");
            report.setLastError(null);
            report.setCompletedAt(Instant.now());
        } catch (RuntimeException exception) {
            log.error("Evidence report generation failed: reportId={}, businessId={}, provider={}",
                    report.getId(), businessId, localMode ? "ollama" : "meta", exception);
            report.setStatus("FAILED");
            String description = exception.getMessage() == null ? "Report generation failed" : exception.getMessage();
            report.setLastError(description.substring(0, Math.min(400, description.length())));
        }
        return toResponse(reports.saveAndFlush(report));
    }

    private EvidenceReport createReport(UUID businessId, String fingerprint) {
        EvidenceReport report = new EvidenceReport();
        report.setBusinessId(businessId);
        report.setSourceFingerprint(fingerprint);
        try {
            return reports.saveAndFlush(report);
        } catch (DataIntegrityViolationException exception) {
            return reports.findByBusinessIdAndSourceFingerprint(businessId, fingerprint)
                    .orElseThrow(() -> exception);
        }
    }

    private EvidenceReportResponse toResponse(EvidenceReport report) {
        SavedReport saved = report.getReportJson() == null ? null
                : json.readValue(report.getReportJson(), SavedReport.class);
        return new EvidenceReportResponse(report.getId(), report.getBusinessId(), report.getStatus(),
                report.getModelName(), report.getModelResponseId(),
                saved == null ? List.of() : saved.sources(),
                saved == null ? List.of() : saved.opportunities(),
                saved == null ? List.of() : saved.plan(),
                saved == null ? List.of() : saved.limitations(),
                saved == null ? List.of() : EvidenceCoverageAssessor.assess(saved.opportunities(), saved.sources()),
                report.getLastError(), report.getCreatedAt(), report.getCompletedAt());
    }

    private String fingerprint(EvidenceSourceService.EvidenceBundle bundle, boolean localMode) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(((localMode ? "LOCAL:" : "META:") + json.writeValueAsString(bundle))
                            .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public static void validateDraft(Draft draft, List<Source> sources) {
        if (draft == null || draft.opportunities() == null || draft.opportunities().size() != 3
                || draft.plan() == null || draft.plan().size() != 7 || draft.limitations() == null) {
            throw new IllegalStateException("AI report must include 3 opportunities and a 7-day plan");
        }
        Set<String> allowed = new HashSet<>();
        for (Source source : sources) allowed.add(source.id());
        Set<Integer> ranks = new HashSet<>();
        for (Opportunity opportunity : draft.opportunities()) {
            if (opportunity == null || !ranks.add(opportunity.rank()) || opportunity.rank() < 1
                    || opportunity.rank() > 3 || blank(opportunity.title()) || blank(opportunity.rationale())
                    || blank(opportunity.uncertainty()) || blank(opportunity.measurement())
                    || opportunity.effort() == null
                    || !List.of("low", "medium", "high").contains(opportunity.effort())) {
                throw new IllegalStateException("AI returned an incomplete opportunity");
            }
            validateRefs(opportunity.evidenceIds(), allowed);
        }
        Set<Integer> days = new HashSet<>();
        for (DayAction action : draft.plan()) {
            if (action == null || action.day() < 1 || action.day() > 7
                    || !days.add(action.day()) || blank(action.task())
                    || blank(action.purpose()) || blank(action.measurement())) {
                throw new IllegalStateException("AI returned an incomplete seven-day plan");
            }
            validateRefs(action.evidenceIds(), allowed);
        }
        if (ranks.size() != 3 || days.size() != 7) {
            throw new IllegalStateException("AI returned duplicate opportunity ranks or days");
        }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private static void validateRefs(List<String> references, Set<String> allowed) {
        if (references == null || references.isEmpty() || references.stream().anyMatch(id -> !allowed.contains(id))) {
            throw new IllegalStateException("AI cited a missing or unrecognized evidence ID");
        }
    }

    private static List<String> mergeLimitations(List<String> observed, List<String> generated) {
        LinkedHashSet<String> unique = new LinkedHashSet<>(observed);
        for (String limitation : generated) {
            if (limitation != null && !limitation.isBlank()) unique.add(limitation);
        }
        return List.copyOf(unique);
    }

    private record SavedReport(List<Source> sources, List<Opportunity> opportunities,
                               List<DayAction> plan, List<String> limitations) {}
}
