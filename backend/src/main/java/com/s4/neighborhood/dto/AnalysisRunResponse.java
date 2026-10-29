package com.s4.neighborhood.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnalysisRunResponse(
        UUID id, String status, String step, String detail,
        String goal, String website, UUID mediaId, UUID reportId,
        boolean useMeta, boolean segmentSign, String lastError,
        Instant createdAt, Instant completedAt, List<WebsiteRecord> websites
) {
    public record WebsiteRecord(String kind, String businessName, String url, String title,
                                String observedSummary, String status, String note, Instant fetchedAt,
                                String osmSourceUrl, String category, Double distanceMiles, String matchLevel) {}
}
