package com.s4.neighborhood.repository;

import com.s4.neighborhood.entity.EvidenceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceReportRepository extends JpaRepository<EvidenceReport, UUID> {
    Optional<EvidenceReport> findTopByBusinessIdOrderByCreatedAtDesc(UUID businessId);
    Optional<EvidenceReport> findByBusinessIdAndSourceFingerprint(UUID businessId, String fingerprint);

    @Modifying
    @Transactional
    @Query("update EvidenceReport r set r.status = 'GENERATING', "
            + "r.attempts = r.attempts + 1, r.lastError = null "
            + "where r.id = :id and r.status in ('PENDING', 'FAILED') and r.attempts < 2")
    int claim(UUID id);

    // Local Ollama retries are owner-triggered and do not use paid model credits.
    // Atomic status check prevents simultaneous requests from claiming the same report.
    @Modifying
    @Transactional
    @Query("update EvidenceReport r set r.status = 'GENERATING', "
            + "r.attempts = r.attempts + 1, r.lastError = null "
            + "where r.id = :id and r.status in ('PENDING', 'FAILED')")
    int claimLocal(UUID id);
}
