package com.s4.neighborhood.dto;

import java.time.Instant;
import java.util.UUID;

public record SegmentationResponse(
        UUID id,
        UUID mediaId,
        String concept,
        String status,
        int attempts,
        String modelName,
        String modelResponseId,
        String rawOutput,
        String lastError,
        Instant createdAt,
        Instant completedAt
) {}
