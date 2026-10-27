
package com.s4.neighborhood.service;

import com.s4.neighborhood.client.OpenStreetMapClient;
import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.entity.NearbyPlace;
import com.s4.neighborhood.entity.NeighborhoodResearch;
import com.s4.neighborhood.repository.BusinessProfileRepository;
import com.s4.neighborhood.repository.NearbyPlaceRepository;
import com.s4.neighborhood.repository.NeighborhoodResearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NeighborhoodResearchServiceTest {
    @Mock BusinessProfileRepository businesses;
    @Mock NeighborhoodResearchRepository researches;
    @Mock NearbyPlaceRepository places;
    @Mock OpenStreetMapClient osm;

    @InjectMocks NeighborhoodResearchService service;

    @Test
    void returnsCachedResearchWithoutExternalCalls() {
        UUID id = UUID.randomUUID();
        BusinessProfile business = business();
        NeighborhoodResearch snapshot = new NeighborhoodResearch();

        snapshot.setFetchedAt(Instant.now());
        snapshot.setRadiusMiles(4);

        when(businesses.findById(id))
                .thenReturn(Optional.of(business));

        when(researches
                .findTopByBusinessIdAndAddressKeyAndRadiusMilesOrderByFetchedAtDesc(
                        eq(id), anyString(), eq(4)))
                .thenReturn(Optional.of(snapshot));

        when(places.findByResearchIdOrderByDistanceMilesAsc(any()))
                .thenReturn(List.of());

        assertTrue(service.research(id).cached());
        verifyNoInteractions(osm);
    }

    @Test
    void calculatesDistanceInMiles() {
        assertEquals(0,
                NeighborhoodResearchService.distanceMiles(
                        33.9, -118.1, 33.9, -118.1));

        assertTrue(
                NeighborhoodResearchService.distanceMiles(
                        33.9, -118.1, 34.0, -118.1) > 6);
    }

    @Test
    void newResearchKeepsOnlyPlacesWithinRadius() {
        UUID id = UUID.randomUUID();
        BusinessProfile business = business();

        OpenStreetMapClient.Location location =
                new OpenStreetMapClient.Location(
                        33.9, -118.1, "Paramount, CA");

        when(businesses.findById(id))
                .thenReturn(Optional.of(business));

        when(researches
                .findTopByBusinessIdAndAddressKeyAndRadiusMilesOrderByFetchedAtDesc(
                        eq(id), anyString(), eq(4)))
                .thenReturn(Optional.empty());

        when(osm.geocode(business)).thenReturn(location);

        when(osm.findNearby(location, 4, "Beauty & Personal Care"))
                .thenReturn(List.of(
                        new OpenStreetMapClient.OsmPlace(
                                "node", 1, "Nearby Salon",
                                "shop: beauty", 33.901, -118.1, ""),
                        new OpenStreetMapClient.OsmPlace(
                                "node", 2, "Far Away",
                                "shop: beauty", 34.2, -118.1, "")
                ));

        when(researches.saveAndFlush(any()))
                .thenAnswer(call -> call.getArgument(0));

        when(places.findByResearchIdOrderByDistanceMilesAsc(any()))
                .thenReturn(List.of());

        service.research(id);

        verify(places).saveAll(argThat(items -> {
            int count = 0;
            for (NearbyPlace ignored : items) count++;
            return count == 1;
        }));
    }

    private static BusinessProfile business() {
        BusinessProfile business = new BusinessProfile();

        business.setName("Golden Brows");
        business.setStreetAddress("15733 Downey Ave");
        business.setCity("Paramount");
        business.setState("CA");
        business.setPostalCode("90723");
        business.setIndustry("Beauty & Personal Care");
        business.setResearchRadiusMiles(4);

        return business;
    }
}
