package com.s4.neighborhood.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AnalysisRunRequest(
        @Size(max = 1000) String goal,
        @Size(max = 500) String website,
        @Size(max = 1500) String customerFeedback,
        UUID mediaId,
        boolean useMeta,
        boolean segmentSign
) {}
