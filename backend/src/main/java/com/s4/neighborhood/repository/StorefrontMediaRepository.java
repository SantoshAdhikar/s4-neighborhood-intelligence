package com.s4.neighborhood.repository;

import com.s4.neighborhood.entity.StorefrontMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorefrontMediaRepository extends JpaRepository<StorefrontMedia, UUID> {
    List<StorefrontMedia> findByBusinessIdOrderByCreatedAtDesc(UUID businessId);
    Optional<StorefrontMedia> findByIdAndBusinessId(UUID id, UUID businessId);

    @Modifying
    @Transactional
    @Query("update StorefrontMedia m set m.status = 'ANALYZING', " +
            "m.analysisAttempts = m.analysisAttempts + 1, m.lastError = null " +
            "where m.id = :mediaId and m.businessId = :businessId " +
            "and m.status in ('UPLOADED', 'FAILED') and m.analysisAttempts < :maxAttempts")
    int claimAnalysis(UUID mediaId, UUID businessId, int maxAttempts);
}
