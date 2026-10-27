
package com.s4.neighborhood.dto;

import java.time.Instant;
import java.util.UUID;

public record BusinessProfileResponse(
        UUID id,
        String name,
        String industry,
        String streetAddress,
        String city,
        String state,
        String postalCode,
        String website,
        String services,
        String primaryGoal,
        int researchRadiusMiles,
        Instant createdAt,
        Instant updatedAt
) {}
