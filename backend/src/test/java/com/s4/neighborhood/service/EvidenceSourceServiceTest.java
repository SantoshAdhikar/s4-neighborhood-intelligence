package com.s4.neighborhood.service;

import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.entity.AnalysisRun;
import com.s4.neighborhood.entity.StorefrontMedia;
import com.s4.neighborhood.dto.VisualFindings;
import com.s4.neighborhood.repository.AnalysisRunRepository;
import com.s4.neighborhood.repository.NearbyPlaceRepository;
import com.s4.neighborhood.repository.NeighborhoodResearchRepository;
import com.s4.neighborhood.repository.StorefrontMediaRepository;
import com.s4.neighborhood.repository.StorefrontSegmentationRepository;
import com.s4.neighborhood.repository.WebsiteFindingRepository;
import com.s4.neighborhood.repository.WebsitePageFindingRepository;
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
    @Mock WebsitePageFindingRepository pageFindings;
    @Mock JsonMapper json;
    @InjectMocks EvidenceSourceService service;

    @Test
    void runWithoutSelectedImageDoesNotReuseAnOlderImage() throws Exception {
        BusinessProfile business = business();
        AnalysisRun run = new AnalysisRun();
        run.setBusinessId(business.getId());
        run.setGoal("Improve neighborhood awareness");
        run.setMediaId(null);
        when(analysisRuns.findByIdAndBusinessId(run.getId(), business.getId())).thenReturn(Optional.of(run));

        var evidence = service.collect(business, false, run.getId());

        assertTrue(evidence.sources().stream().noneMatch(source ->
                "STORE_FRONT_IMAGE".equals(source.kind())));
        assertTrue(evidence.sources().stream().anyMatch(source ->
                "profile-goal".equals(source.id()) && source.description().contains(run.getGoal())));
        verify(media, never()).findByBusinessIdOrderByCreatedAtDesc(any());
        verify(analysisRuns, never()).findTopByBusinessIdOrderByCreatedAtDesc(any());
        verify(websiteFindings, atLeastOnce()).findByRunIdOrderByFetchedAtAsc(run.getId());
    }

    @Test
    void runIncludesOnlyItsSelectedAnalyzedImage() throws Exception {
        BusinessProfile business = business();
        AnalysisRun run = new AnalysisRun();
        run.setBusinessId(business.getId());
        UUID photoId = UUID.randomUUID();
        run.setMediaId(photoId);
        StorefrontMedia selectedPhoto = new StorefrontMedia();
        var photoIdField = StorefrontMedia.class.getDeclaredField("id");
        photoIdField.setAccessible(true);
        photoIdField.set(selectedPhoto, photoId);
        selectedPhoto.setBusinessId(business.getId());
        selectedPhoto.setOriginalFilename("current.jpg");
        selectedPhoto.setStatus("ANALYZED");
        selectedPhoto.setAnalysisJson("{}");
        when(analysisRuns.findByIdAndBusinessId(run.getId(), business.getId())).thenReturn(Optional.of(run));
        when(media.findById(photoId)).thenReturn(Optional.of(selectedPhoto));
        when(json.readValue("{}", VisualFindings.class)).thenReturn(new VisualFindings(
                List.of(new VisualFindings.Observation("signage", "Visible lettering", null, "high")),
                List.of()));

        var evidence = service.collect(business, false, run.getId());

        assertEquals(1, evidence.sources().stream().filter(source ->
                "STORE_FRONT_IMAGE".equals(source.kind())).count());
        assertTrue(evidence.sources().stream().anyMatch(source ->
                source.description().contains("current.jpg")));
        verify(media, never()).findByBusinessIdOrderByCreatedAtDesc(any());
    }

    private static BusinessProfile business() throws Exception {
        BusinessProfile business = new BusinessProfile();
        var field = BusinessProfile.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(business, UUID.randomUUID());
        business.setStreetAddress("100 Main St");
        business.setCity("Paramount");
        business.setState("CA");
        business.setPostalCode("90723");
        business.setIndustry("Retail");
        business.setResearchRadiusMiles(4);
        business.setPrimaryGoal("Grow the business");
        business.setServices("Groceries");
        return business;
    }

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
