
package com.s4.neighborhood.repository;

import com.s4.neighborhood.entity.BusinessProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface BusinessProfileRepository
        extends JpaRepository<BusinessProfile, UUID> {
    List<BusinessProfile> findByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId);
    boolean existsByIdAndOwnerUserId(UUID id, UUID ownerUserId);
    boolean existsByOwnerUserId(UUID ownerUserId);
}
