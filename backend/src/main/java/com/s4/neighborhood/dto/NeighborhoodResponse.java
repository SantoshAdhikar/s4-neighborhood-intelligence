
package com.s4.neighborhood.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NeighborhoodResponse(
        UUID businessId,
        String businessName,
        int radiusMiles,
        double latitude,
        double longitude,
        String matchedAddress,
        Instant fetchedAt,
        boolean cached,
        List<NearbyBusinessResponse> places
) {
    public record NearbyBusinessResponse(
            String osmType,
            long osmId,
            String name,
            String category,
            double latitude,
            double longitude,
            double distanceMiles,
            String website,
            String sourceUrl
    ) {}
}
