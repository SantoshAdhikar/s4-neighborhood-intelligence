package com.s4.neighborhood.service;

import com.s4.neighborhood.dto.EvidenceReportResponse;
import com.s4.neighborhood.entity.AnalysisRun;
import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.repository.AnalysisRunRepository;
import com.s4.neighborhood.repository.BusinessProfileRepository;
import com.s4.neighborhood.repository.WebsiteFindingRepository;
import com.s4.neighborhood.repository.WebsitePageFindingRepository;
import org.junit.jupiter.api.Test;
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
    @InjectMocks AnalysisRunService service;

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
        when(reports.generateLocal(businessId)).thenReturn(new EvidenceReportResponse(
                reportId, businessId, "COMPLETED", "ollama/qwen2.5:7b", null,
                null, null, null, null, null, null, null));

        service.process(run.getId());

        verify(websiteResearch).research(run, business);
        verify(reports).generateLocal(businessId);
        assertEquals("COMPLETED", run.getStatus());
        assertEquals(reportId, run.getReportId());
        assertTrue(run.getDetail().contains("Neighborhood discovery was unavailable"));
    }
}
