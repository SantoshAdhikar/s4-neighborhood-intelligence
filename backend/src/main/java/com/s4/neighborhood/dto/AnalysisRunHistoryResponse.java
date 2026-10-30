package com.s4.neighborhood.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A lightweight, paged index of saved analysis runs for one authorized business. */
public record AnalysisRunHistoryResponse(List<Item> items, int page, boolean hasMore) {
    public record Item(UUID id, String status, String goal, Instant createdAt,
                       Instant completedAt, UUID reportId, boolean useMeta) {}
}
