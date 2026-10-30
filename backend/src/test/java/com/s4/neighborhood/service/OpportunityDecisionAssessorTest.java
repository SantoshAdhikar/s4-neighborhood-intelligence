package com.s4.neighborhood.service;

import com.s4.neighborhood.dto.EvidenceReportResponse.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class OpportunityDecisionAssessorTest {
    @Test
    void ranksTestableObservedActionAheadOfUnverifiedAdviceWithoutClaimingRevenue() {
        List<Opportunity> proposals = List.of(
                new Opportunity(1, "Increase advertising", "Ads will attract customers", List.of("profile-goal"), "Unknown ROI", "low", "Track calls"),
                new Opportunity(2, "Fix brow lamination booking page", "Improve brow lamination bookings", List.of("profile-goal", "page-1"), "Conversion unknown", "medium", "Track bookings"),
                new Opportunity(3, "Open new store", "Long project", List.of("page-1"), "Costs unknown", "high", "Track feasibility"));
        List<Source> sources = List.of(
                new Source("profile-goal", "OWNER_PROFILE", "Owner-stated goal: more brow lamination bookings", "/profile", Instant.now()),
                new Source("page-1", "WEBSITE_PAGE", "Sampled website booking page", "https://example.com/book", Instant.now()));
        List<EvidenceCoverage> coverage = EvidenceCoverageAssessor.assess(proposals, sources);
        List<ActionReadiness> result = OpportunityDecisionAssessor.assess(proposals, sources, coverage);
        assertEquals(2, result.getFirst().modelRank());
        assertEquals("READY_TO_TEST", result.getFirst().readiness());
        assertEquals(3, result.get(1).modelRank());
        assertEquals("VERIFY_FIRST", result.get(2).readiness());
        assertEquals(1, result.get(2).modelRank());
        assertFalse(result.getFirst().reason().toLowerCase().contains("guaranteed profit"));
    }
}
