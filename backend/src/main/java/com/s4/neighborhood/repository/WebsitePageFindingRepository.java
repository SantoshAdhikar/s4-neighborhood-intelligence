package com.s4.neighborhood.repository;

import com.s4.neighborhood.entity.WebsitePageFinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WebsitePageFindingRepository extends JpaRepository<WebsitePageFinding, UUID> {
    List<WebsitePageFinding> findByRunIdOrderByFetchedAtAsc(UUID runId);
}
