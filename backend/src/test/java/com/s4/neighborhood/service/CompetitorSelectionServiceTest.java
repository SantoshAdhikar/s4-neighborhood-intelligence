package com.s4.neighborhood.service;

import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.entity.NearbyPlace;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompetitorSelectionServiceTest {
    @Test
    void selectsTenMatchingBusinessesRatherThanNearestUnrelatedStores() {
        var business = business("Beauty & Personal Care");
        var places = new ArrayList<NearbyPlace>();
        places.add(place("Convenience Grocery", "shop: convenience", 0.02));
        places.add(place("Hair Salon", "shop: hairdresser", 0.01));
        for (int i = 0; i < 12; i++) {
            places.add(place("Brow and Lash Studio " + i, "shop: beauty", 0.2 + i * 0.01));
        }
        var matches = CompetitorSelectionService.select(business, places);
        assertEquals(10, matches.size());
        assertTrue(matches.stream().allMatch(item -> item.matchLevel().equals("SAME_INDUSTRY")));
        assertTrue(matches.stream().noneMatch(item -> item.place().getName().contains("Grocery")));
        assertTrue(matches.stream().noneMatch(item -> item.place().getName().equals("Hair Salon")));
    }

    @Test
    void includesRelatedBusinessesOnlyWhenTooFewSameIndustryMatchesExist() {
        var business = business("Beauty & Personal Care");
        var matches = CompetitorSelectionService.select(business, List.of(
                place("Brows Studio", "shop: beauty", 1.2),
                place("BrowS Studio", "shop: beauty", 1.5), // duplicate listing name
                place("Nail Spa", "amenity: spa", 0.1),
                place("Bike Repair", "shop: bicycle", 0.05)));
        assertEquals(2, matches.size());
        assertEquals("Brows Studio", matches.get(0).place().getName());
        assertEquals("SAME_INDUSTRY", matches.get(0).matchLevel());
        assertEquals("RELATED_INDUSTRY", matches.get(1).matchLevel());
    }

    @Test
    void restaurantMatchesFoodBusinessesAndExcludesUnrelatedListings() {
        var business = business("Restaurant & Food");
        var matches = CompetitorSelectionService.select(business, List.of(
                place("Bistro", "amenity: restaurant", 0.8),
                place("Pharmacy", "amenity: pharmacy", 0.1),
                place("Coffee Bar", "amenity: cafe", 1.4)));
        assertEquals(2, matches.size());
    }

    private static BusinessProfile business(String industry) {
        BusinessProfile profile = new BusinessProfile();
        profile.setName("Golden Brows Threading & Beauty Studio");
        profile.setIndustry(industry);
        return profile;
    }

    private static NearbyPlace place(String name, String category, double distance) {
        NearbyPlace place = new NearbyPlace();
        place.setName(name);
        place.setCategory(category);
        place.setDistanceMiles(distance);
        return place;
    }
}
