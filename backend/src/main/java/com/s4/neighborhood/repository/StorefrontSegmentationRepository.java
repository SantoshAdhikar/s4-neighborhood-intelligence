package com.s4.neighborhood.repository;

import com.s4.neighborhood.entity.StorefrontSegmentation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorefrontSegmentationRepository extends JpaRepository<StorefrontSegmentation, UUID> {
    List<StorefrontSegmentation> findByMediaIdOrderByCreatedAtDesc(UUID mediaId);
    Optional<StorefrontSegmentation> findByMediaIdAndConcept(UUID mediaId, String concept);

    @Modifying
    @Transactional
    @Query("update StorefrontSegmentation s set s.status = 'PROCESSING', "
            + "s.attempts = s.attempts + 1, s.lastError = null "
            + "where s.id = :id and s.status in ('PENDING', 'FAILED') and s.attempts < :maxAttempts")
    int claim(UUID id, int maxAttempts);
}
