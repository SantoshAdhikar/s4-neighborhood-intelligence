package com.s4.neighborhood.repository;

import com.s4.neighborhood.entity.AnalysisRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnalysisRunRepository extends JpaRepository<AnalysisRun, UUID> {
    Optional<AnalysisRun> findTopByBusinessIdOrderByCreatedAtDesc(UUID businessId);
    Page<AnalysisRun> findByBusinessId(UUID businessId, Pageable pageable);
    List<AnalysisRun> findByBusinessIdAndIdIn(UUID businessId, List<UUID> ids);
    Optional<AnalysisRun> findByIdAndBusinessId(UUID id, UUID businessId);
    boolean existsByBusinessIdAndStatusIn(UUID businessId, List<String> statuses);
    List<AnalysisRun> findByStatusIn(List<String> statuses);
}
