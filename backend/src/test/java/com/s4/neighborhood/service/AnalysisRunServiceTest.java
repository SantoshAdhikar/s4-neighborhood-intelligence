package com.s4.neighborhood.service;

import com.s4.neighborhood.dto.EvidenceReportResponse;
import com.s4.neighborhood.entity.AnalysisRun;
import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.repository.AnalysisRunRepository;
import com.s4.neighborhood.repository.BusinessProfileRepository;
import com.s4.neighborhood.repository.WebsiteFindingRepository;
import com.s4.neighborhood.repository.WebsitePageFindingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisRunServiceTest {
    @Mock AnalysisRunRepository runs;
    @Mock WebsiteFindingRepository websites;
    @Mock WebsitePageFindingRepository pages;
    @Mock BusinessProfileRepository businesses;
    @Mock StorefrontMediaService media;
    @Mock NeighborhoodResearchService neighborhood;
    @Mock WebsiteResearchService websiteResearch;
    @Mock StorefrontSegmentationService segmentation;
    @Mock EvidenceReportService reports;
    @Mock AnalysisModelEvidenceService modelEvidence;
    @InjectMocks AnalysisRunService service;

    @Test
    void historyReturnsOnlyLightweightBusinessScopedSummaries() {
        UUID businessId = UUID.randomUUID();
        AnalysisRun run = new AnalysisRun();
        run.setBusinessId(businessId);
        run.setGoal("Grow local bookings");
        run.setStatus("COMPLETED");
        UUID reportId = UUID.randomUUID();
        run.setReportId(reportId);
        when(runs.findByBusinessId(eq(businessId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(run), PageRequest.of(0, 20), 21));

        var result = service.history(businessId, 0);

        assertEquals(1, result.items().size());
        assertEquals(run.getId(), result.items().getFirst().id());
        assertEquals(reportId, result.items().getFirst().reportId());
        assertEquals("Grow local bookings", result.items().getFirst().goal());
        assertTrue(result.hasMore());
        var paging = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(runs).findByBusinessId(eq(businessId), paging.capture());
        assertEquals(20, paging.getValue().getPageSize());
        assertEquals(Sort.Direction.DESC,
                paging.getValue().getSort().getOrderFor("createdAt").getDirection());
        verifyNoInteractions(websites, pages, reports);
    }

    @Test
    void historyRejectsInvalidPagesBeforeAccessingDatabase() {
        var error = assertThrows(ResponseStatusException.class,
                () -> service.history(UUID.randomUUID(), -1));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(runs);
    }

    @Test
    void readingAnotherBusinessRunReturnsNotFound() {
        UUID businessId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        when(runs.findByIdAndBusinessId(runId, businessId)).thenReturn(Optional.empty());
        var error = assertThrows(ResponseStatusException.class,
                () -> service.get(businessId, runId));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(websites, pages);
    }

    @Test
    void proceedsToWebsiteAndReportWhenNeighborhoodProviderUnavailableAndNoSnapshotExists() {
        UUID businessId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        AnalysisRun run = new AnalysisRun();
        run.setBusinessId(businessId);
        run.setGoal("Increase local discovery");
        run.setWebsite("https://example.com");
        BusinessProfile business = new BusinessProfile();
        business.setName("Local Shop");
        business.setIndustry("Retail");

        when(runs.findById(run.getId())).thenReturn(Optional.of(run));
        when(businesses.findById(businessId)).thenReturn(Optional.of(business));
        when(businesses.saveAndFlush(business)).thenReturn(business);
        when(runs.saveAndFlush(run)).thenReturn(run);
        when(neighborhood.research(businessId)).thenThrow(
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OSM unavailable"));
        when(neighborhood.hasSavedSnapshot(businessId)).thenReturn(false);
        when(reports.generateLocal(businessId, run.getId())).thenReturn(new EvidenceReportResponse(
                reportId, businessId, "COMPLETED", "ollama/qwen2.5:7b", null,
                null, null, null, null, null, null, null, null, null));

        service.process(run.getId());

        verify(websiteResearch).research(run, business);
        verify(reports).generateLocal(businessId, run.getId());
        assertEquals("COMPLETED", run.getStatus());
        assertEquals(reportId, run.getReportId());
        assertTrue(run.getDetail().contains("Neighborhood discovery was unavailable"));
    }
    @Test
    void removesOnlySelectedCompletedBusinessRuns() {
        UUID businessId = UUID.randomUUID();
        AnalysisRun complete = new AnalysisRun();
        complete.setBusinessId(businessId);
        complete.setStatus("COMPLETED");
        when(runs.findByIdAndBusinessId(complete.getId(), businessId))
                .thenReturn(Optional.of(complete));

        service.deleteHistory(businessId, List.of(complete.getId()));

        verify(runs).deleteAllInBatch(List.of(complete));
        verifyNoInteractions(media, reports, neighborhood);
    }

    @Test
    void refusesToDeleteAnActiveRun() {
        UUID businessId = UUID.randomUUID();
        AnalysisRun running = new AnalysisRun();
        running.setBusinessId(businessId);
        running.setStatus("RUNNING");
        when(runs.findByIdAndBusinessId(running.getId(), businessId))
                .thenReturn(Optional.of(running));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.deleteHistory(businessId, List.of(running.getId())));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(runs, never()).deleteAllInBatch(anyList());
    }

    @Test
    void refusesCrossBusinessAndDuplicateBatchWithoutDeleting() {
        UUID businessId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        when(runs.findByIdAndBusinessId(runId, businessId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.deleteHistory(businessId, List.of(runId))).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.deleteHistory(businessId, List.of(runId, runId))).getStatusCode());
        verify(runs, never()).deleteAllInBatch(anyList());
    }


    @Test
    void refusesNullRunIdWithoutQueryingOrDeleting() {
        UUID businessId = UUID.randomUUID();
        List<UUID> ids = new java.util.ArrayList<>();
        ids.add(UUID.randomUUID());
        ids.add(null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.deleteHistory(businessId, ids));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(runs);
    }

    @Test
    void refusesMixedBusinessBatchWithoutPartialDeletion() {
        UUID businessId = UUID.randomUUID();
        AnalysisRun owned = new AnalysisRun();
        owned.setBusinessId(businessId);
        owned.setStatus("COMPLETED");
        UUID notOwnedId = UUID.randomUUID();
        when(runs.findByIdAndBusinessId(owned.getId(), businessId)).thenReturn(Optional.of(owned));
        when(runs.findByIdAndBusinessId(notOwnedId, businessId)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.deleteHistory(businessId, List.of(owned.getId(), notOwnedId)));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(runs, never()).deleteAllInBatch(anyList());
    }

}
