package com.s4.neighborhood.service;

import com.s4.neighborhood.dto.EvidenceReportResponse;
import com.s4.neighborhood.dto.EvidenceReportResponse.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EvidenceReportServiceTest {
    private static final Source VERIFIED = new Source("visual-1", "STORE_FRONT_IMAGE",
            "Observed storefront lettering", "/businesses/1/storefront", Instant.now());

    @Test
    void acceptsThreePrioritiesAndSevenDaysWithKnownEvidence() {
        assertDoesNotThrow(() -> EvidenceReportService.validateDraft(draft("visual-1"), List.of(VERIFIED)));
    }

    @Test
    void rejectsUnknownSourceReferences() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> EvidenceReportService.validateDraft(draft("invented-source"), List.of(VERIFIED)));
        assertTrue(exception.getMessage().contains("missing or unrecognized"));
    }

    @Test
    void rejectsMissingDays() {
        Draft original = draft("visual-1");
        Draft incomplete = new Draft(original.opportunities(), original.plan().subList(0, 6), List.of());
        assertThrows(IllegalStateException.class,
                () -> EvidenceReportService.validateDraft(incomplete, List.of(VERIFIED)));
    }

    private static Draft draft(String evidenceId) {
        List<Opportunity> opportunities = java.util.stream.IntStream.rangeClosed(1, 3)
                .mapToObj(rank -> new Opportunity(rank, "Test improvement", "Rationale",
                        List.of(evidenceId), "Requires owner verification", "low", "Count sign inquiries"))
                .toList();
        List<DayAction> plan = java.util.stream.IntStream.rangeClosed(1, 7)
                .mapToObj(day -> new DayAction(day, "Inspect visible sign", "Confirm visibility",
                        List.of(evidenceId), "Log owner observations"))
                .toList();
        return new Draft(opportunities, plan, List.of("No customer traffic data"));
    }
}
