package com.s4.neighborhood.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnalysisRunResponse(
        UUID id, String status, String step, String detail,
        String goal, String website, String focusServices, UUID mediaId, UUID reportId,
        boolean useMeta, boolean segmentSign, String lastError,
        Instant createdAt, Instant completedAt, List<WebsiteRecord> websites, List<PageRecord> pages,
        List<ModelEvidence> modelEvidence
) {
    /** Populated only from saved provider records; status never implies a model call on its own. */
    public record ModelEvidence(String task, String provider, String status, String modelName,
                                String responseId, Instant completedAt, String detail) {}

    public record PageRecord(UUID websiteFindingId, String pageType, String pageUrl, String pageTitle,
                             String status, String observedSummary, String note, Instant fetchedAt) {}

    public record WebsiteRecord(UUID id, String kind, String businessName, String url, String title,
                                String observedSummary, String status, String note, Instant fetchedAt,
                                String osmSourceUrl, String category, Double distanceMiles, String matchLevel) {}
}
