package com.s4.neighborhood.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnalysisRunResponse(
        UUID id, String status, String step, String detail,
        String goal, String website, String focusServices, UUID mediaId, UUID reportId,
        boolean useMeta, boolean segmentSign, String lastError,
        Instant createdAt, Instant completedAt, List<WebsiteRecord> websites, List<PageRecord> pages
) {
    public record PageRecord(UUID websiteFindingId, String pageType, String pageUrl, String pageTitle,
                             String status, String observedSummary, String note, Instant fetchedAt) {}

    public record WebsiteRecord(UUID id, String kind, String businessName, String url, String title,
                                String observedSummary, String status, String note, Instant fetchedAt,
                                String osmSourceUrl, String category, Double distanceMiles, String matchLevel) {}
}
