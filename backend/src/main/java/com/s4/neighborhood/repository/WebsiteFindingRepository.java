package com.s4.neighborhood.repository;

import com.s4.neighborhood.entity.WebsiteFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WebsiteFindingRepository extends JpaRepository<WebsiteFinding, UUID> {
    List<WebsiteFinding> findByRunIdOrderByFetchedAtAsc(UUID runId);
}
