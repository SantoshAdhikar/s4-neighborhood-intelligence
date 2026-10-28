package com.s4.neighborhood.dto;

import java.time.Instant;
import java.util.UUID;

public record StorefrontMediaResponse(
        UUID id,
        UUID businessId,
        String originalFilename,
        String mimeType,
        long sizeBytes,
        String sha256,
        String status,
        int analysisAttempts,
        String modelName,
        String modelResponseId,
        VisualFindings findings,
        String lastError,
        Instant createdAt,
        Instant analyzedAt
) {}
