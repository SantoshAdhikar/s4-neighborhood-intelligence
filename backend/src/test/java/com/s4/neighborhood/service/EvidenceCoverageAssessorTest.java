package com.s4.neighborhood.service;

import com.s4.neighborhood.dto.EvidenceReportResponse.Opportunity;
import com.s4.neighborhood.dto.EvidenceReportResponse.Source;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EvidenceCoverageAssessorTest {
    @Test
    void observedSourcesAreDistinguishedFromOwnerInputs() {
        var result = EvidenceCoverageAssessor.assess(List.of(opportunity(1, "owner", "page", "osm")), List.of(
                source("owner", "OWNER_PROFILE"), source("page", "WEBSITE_PAGE"), source("osm", "OSM_PLACE")));
        assertEquals("OBSERVED", result.getFirst().level());
        assertEquals(1, result.getFirst().observedSources());
        assertEquals(1, result.getFirst().contextualSources());
        assertEquals(1, result.getFirst().ownerSources());
    }

    @Test
    void osmListingDoesNotBecomeProofOfCompetitorPerformance() {
        var result = EvidenceCoverageAssessor.assess(List.of(opportunity(2, "osm")),
                List.of(source("osm", "OSM_PLACE")));
        assertEquals("CONTEXT", result.getFirst().level());
    }

    @Test
    void feedbackAndProfileAreLabeledOwnerOnly() {
        var result = EvidenceCoverageAssessor.assess(List.of(opportunity(3, "owner", "feedback")),
                List.of(source("owner", "OWNER_PROFILE"), source("feedback", "OWNER_FEEDBACK")));
        assertEquals("OWNER_ONLY", result.getFirst().level());
    }

    @Test
    void duplicateReferencesDoNotInflateCounts() {
        var result = EvidenceCoverageAssessor.assess(List.of(opportunity(1, "page", "page")),
                List.of(source("page", "NEARBY_COMPETITOR")));
        assertEquals(1, result.getFirst().observedSources());
    }

    private static Opportunity opportunity(int rank, String... references) {
        return new Opportunity(rank, "Test", "Rationale", List.of(references), "Unknown", "low", "Track");
    }

    private static Source source(String id, String kind) {
        return new Source(id, kind, "Observed text", "https://example.com", Instant.now());
    }
}
