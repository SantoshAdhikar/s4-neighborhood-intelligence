
package com.s4.neighborhood.repository;

import com.s4.neighborhood.entity.NearbyPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NearbyPlaceRepository extends JpaRepository<NearbyPlace, UUID> {
    List<NearbyPlace> findByResearchIdOrderByDistanceMilesAsc(UUID researchId);
}
