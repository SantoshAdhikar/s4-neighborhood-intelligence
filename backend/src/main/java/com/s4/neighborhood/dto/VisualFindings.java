package com.s4.neighborhood.dto;

import java.util.List;

public record VisualFindings(
        List<Observation> observations,
        List<String> limitations
) {
    public record Observation(
            String aspect,
            String visibleEvidence,
            String videoTimestamp,
            String certainty
    ) {}
}
