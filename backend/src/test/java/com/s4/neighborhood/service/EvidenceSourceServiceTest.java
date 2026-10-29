package com.s4.neighborhood.service;

import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.repository.AnalysisRunRepository;
import com.s4.neighborhood.repository.NearbyPlaceRepository;
import com.s4.neighborhood.repository.NeighborhoodResearchRepository;
import com.s4.neighborhood.repository.StorefrontMediaRepository;
import com.s4.neighborhood.repository.StorefrontSegmentationRepository;
import com.s4.neighborhood.repository.WebsiteFindingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvidenceSourceServiceTest {
    @Mock NeighborhoodResearchRepository researches;
    @Mock NearbyPlaceRepository places;
    @Mock StorefrontMediaRepository media;
    @Mock StorefrontSegmentationRepository segmentations;
    @Mock AnalysisRunRepository analysisRuns;
    @Mock WebsiteFindingRepository websiteFindings;
    @Mock JsonMapper json;
    @InjectMocks EvidenceSourceService service;

    @Test
    void missingNeighborhoodSnapshotIsDisclosedWithoutRequiringManualResearch() throws Exception {
        BusinessProfile business = new BusinessProfile();
        UUID businessId = UUID.randomUUID();
        business.setStreetAddress("100 Main St");
        business.setCity("Paramount");
        business.setState("CA");
        business.setPostalCode("90723");
        business.setIndustry("Retail");
        business.setResearchRadiusMiles(4);
        business.setPrimaryGoal("Grow the business");
        business.setServices("Groceries");
        var field = BusinessProfile.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(business, businessId);
        when(media.findByBusinessIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(researches.findTopByBusinessIdAndAddressKeyAndRadiusMilesOrderByFetchedAtDesc(
                any(), anyString(), eq(4))).thenReturn(Optional.empty());
        when(analysisRuns.findTopByBusinessIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        var bundle = service.collect(business, false);

        assertEquals(3, bundle.sources().size());
        assertTrue(bundle.limitations().stream().anyMatch(item ->
                item.contains("No neighborhood snapshot is available")));
        assertTrue(bundle.limitations().stream().anyMatch(item ->
                item.contains("No analyzed storefront image")));
    }
}
