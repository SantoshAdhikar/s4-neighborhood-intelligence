
package com.s4.neighborhood.repository;

import com.s4.neighborhood.entity.NeighborhoodResearch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface NeighborhoodResearchRepository extends JpaRepository<NeighborhoodResearch, UUID> {
    Optional<NeighborhoodResearch> findTopByBusinessIdOrderByFetchedAtDesc(UUID businessId);
    Optional<NeighborhoodResearch> findTopByBusinessIdAndAddressKeyAndRadiusMilesOrderByFetchedAtDesc(
            UUID businessId, String addressKey, int radiusMiles);
}
