package com.s4.neighborhood.service;

import com.s4.neighborhood.dto.AnalysisRunResponse.ModelEvidence;
import com.s4.neighborhood.entity.AnalysisRun;
import com.s4.neighborhood.entity.EvidenceReport;
import com.s4.neighborhood.entity.StorefrontMedia;
import com.s4.neighborhood.entity.StorefrontSegmentation;
import com.s4.neighborhood.repository.EvidenceReportRepository;
import com.s4.neighborhood.repository.StorefrontMediaRepository;
import com.s4.neighborhood.repository.StorefrontSegmentationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Reconstructs what saved model records can actually establish for one analysis run. */
@Service
public class AnalysisModelEvidenceService {
    private final StorefrontMediaRepository media;
    private final StorefrontSegmentationRepository segmentations;
    private final EvidenceReportRepository reports;

    public AnalysisModelEvidenceService(StorefrontMediaRepository media,
                                        StorefrontSegmentationRepository segmentations,
                                        EvidenceReportRepository reports) {
        this.media = media;
        this.segmentations = segmentations;
        this.reports = reports;
    }

    public List<ModelEvidence> forRun(AnalysisRun run) {
        return List.of(visual(run), segmentation(run), report(run));
    }

    private ModelEvidence visual(AnalysisRun run) {
        if (!run.isUseMeta()) return unrequested("VISUAL", "Meta Model API",
                "Local-only mode did not request a Meta visual inspection.");
        if (run.getMediaId() == null) return unrequested("VISUAL", "Meta Model API",
                "No storefront media was attached to this analysis.");
        StorefrontMedia record = media.findByIdAndBusinessId(run.getMediaId(), run.getBusinessId()).orElse(null);
        if (record == null) return missing("VISUAL", "Meta Model API", run,
                "The selected image record is unavailable; a visual model call cannot be verified.");
        if ("FAILED".equals(record.getStatus())) return new ModelEvidence("VISUAL", "Meta Model API", "FAILED",
                null, null, null, "Saved visual inspection failed. See the Visual Inspection page for details.");
        if (!"ANALYZED".equals(record.getStatus())) return missing("VISUAL", "Meta Model API", run,
                "No completed visual response is recorded for the selected image.");
        return completed("VISUAL", "Meta Model API", run,
                record.getModelName(), record.getModelResponseId(), record.getAnalyzedAt(), true,
                "Storefront visual observations saved for the selected image.");
    }

    private ModelEvidence segmentation(AnalysisRun run) {
        if (!run.isUseMeta() || !run.isSegmentSign()) return unrequested("SEGMENTATION", "Meta Model API",
                "Sign segmentation was not requested for this run.");
        UUID mediaId = run.getMediaId();
        if (mediaId == null) return unrequested("SEGMENTATION", "Meta Model API",
                "No storefront image was selected for segmentation.");
        StorefrontMedia selected = media.findByIdAndBusinessId(mediaId, run.getBusinessId()).orElse(null);
        if (selected == null) return missing("SEGMENTATION", "Meta Model API", run,
                "The selected image record is unavailable.");
        if (!selected.getMimeType().startsWith("image/") || selected.getSizeBytes() > 8_388_608) {
            return new ModelEvidence("SEGMENTATION", "Meta Model API", "SKIPPED", null, null, null,
                    "This workflow only segments JPEG/PNG images of 8 MB or less.");
        }
        StorefrontSegmentation record = segmentations.findByMediaIdAndConcept(mediaId, "storefront sign").orElse(null);
        if (record == null) return missing("SEGMENTATION", "Meta Model API", run,
                "No stored SAM result is linked to the selected photo and storefront sign concept.");
        if ("FAILED".equals(record.getStatus())) return new ModelEvidence("SEGMENTATION", "Meta Model API",
                "FAILED", null, null, null, "Saved segmentation failed. See Visual Inspection for details.");
        if (!"COMPLETED".equals(record.getStatus())) return missing("SEGMENTATION", "Meta Model API", run,
                "A completed segmentation response has not been recorded.");
        return completed("SEGMENTATION", "Meta Model API", run,
                record.getModelName(), record.getModelResponseId(), record.getCompletedAt(), true,
                "A saved storefront-sign segmentation result exists. Verify its actual mask visually.");
    }

    private ModelEvidence report(AnalysisRun run) {
        String provider = run.isUseMeta() ? "Meta Model API" : "Local Ollama";
        if (run.getReportId() == null) return missing("RECOMMENDATIONS", provider, run,
                "No linked, completed recommendation report is recorded for this run.");
        EvidenceReport record = reports.findById(run.getReportId())
                .filter(item -> run.getBusinessId().equals(item.getBusinessId())).orElse(null);
        if (record == null) return missing("RECOMMENDATIONS", provider, run,
                "The linked report is unavailable for this business.");
        if ("FAILED".equals(record.getStatus())) return new ModelEvidence("RECOMMENDATIONS", provider,
                "FAILED", null, null, null, "Report generation failed.");
        if (!"COMPLETED".equals(record.getStatus())) return missing("RECOMMENDATIONS", provider, run,
                "The linked report has not completed.");
        return completed("RECOMMENDATIONS", provider, run,
                record.getModelName(), record.getModelResponseId(), record.getCompletedAt(), run.isUseMeta(),
                run.isUseMeta() ? "Saved Meta-generated recommendations, linked to this analysis."
                        : "Saved local Ollama recommendations; local model outputs have no Meta API response ID.");
    }

    private static ModelEvidence completed(String task, String provider, AnalysisRun run,
                                           String model, String responseId, Instant finished,
                                           boolean remote, String detail) {
        if (blank(model) || finished == null || remote && blank(responseId)) {
            return new ModelEvidence(task, provider, "UNVERIFIED", null, null, null,
                    "A completed record exists but required model provenance is missing.");
        }
        if (run.getCompletedAt() != null && finished.isAfter(run.getCompletedAt())) {
            return new ModelEvidence(task, provider, "UNVERIFIED", null, null, null,
                    "The model record was completed after this analysis ended; it cannot prove this run used it.");
        }
        boolean reused = finished.isBefore(run.getCreatedAt());
        return new ModelEvidence(task, provider, reused ? "REUSED" : "COMPLETED",
                model, remote ? responseId : null, finished,
                detail + (reused ? " This model result existed before this run; no new call is established." : ""));
    }

    private static ModelEvidence missing(String task, String provider, AnalysisRun run, String reason) {
        String status = "QUEUED".equals(run.getStatus()) || "RUNNING".equals(run.getStatus())
                ? "PENDING" : "NOT_RECORDED";
        return new ModelEvidence(task, provider, status, null, null, null, reason);
    }

    private static ModelEvidence unrequested(String task, String provider, String reason) {
        return new ModelEvidence(task, provider, "NOT_REQUESTED", null, null, null, reason);
    }

    private static boolean blank(String text) { return text == null || text.isBlank(); }
}
