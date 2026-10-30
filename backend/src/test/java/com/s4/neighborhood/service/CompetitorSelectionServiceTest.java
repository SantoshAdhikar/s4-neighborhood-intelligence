package com.s4.neighborhood.service;

import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.entity.NearbyPlace;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompetitorSelectionServiceTest {
    @Test
    void selectsFiftyMatchingBusinessesRatherThanNearestUnrelatedStores() {
        var business = business("Beauty & Personal Care");
        var places = new ArrayList<NearbyPlace>();
        places.add(place("Convenience Grocery", "shop: convenience", 0.02));
        places.add(place("Hair Salon", "shop: hairdresser", 0.01));
        for (int i = 0; i < 65; i++) {
            places.add(place("Brow and Lash Studio " + i, "shop: beauty", 0.2 + i * 0.01));
        }
        var matches = CompetitorSelectionService.select(business, places);
        assertEquals(50, matches.size());
        assertTrue(matches.stream().allMatch(item -> item.matchLevel().equals("DIRECT_SPECIALTY")));
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
        assertEquals("DIRECT_SPECIALTY", matches.get(0).matchLevel());
        assertEquals("RELATED_INDUSTRY", matches.get(1).matchLevel());
    }

    @Test
    void prioritizesThreadingSpecialistsOverCloserGeneralBeautyAndNailShops() {
        var business = business("Beauty & Personal Care");
        business.setServices("Eyebrow threading, brow lamination, keratin lash lift, waxing, facials");
        var matches = CompetitorSelectionService.select(business, List.of(
                place("Generic Beauty Salon", "shop: beauty", 0.08),
                place("Nail Art Studio", "shop: beauty", 0.09),
                place("Rara Threading", "shop: beauty", 1.9),
                place("N Threading Salon", "shop: beauty", 2.0),
                place("Rewa Threading", "shop: beauty", 1.7),
                place("Hair Salon", "shop: hairdresser", 0.06)));
        assertEquals(6, matches.size());
        assertEquals(List.of("Rewa Threading", "Rara Threading", "N Threading Salon"),
                matches.subList(0, 3).stream().map(c -> c.place().getName()).toList());
        assertTrue(matches.subList(0, 3).stream().allMatch(c -> "DIRECT_SPECIALTY".equals(c.matchLevel())));
        assertEquals("POTENTIAL_OVERLAP", matches.get(3).matchLevel());
    }

    @Test
    void focusCanChooseLashesInsteadOfThreadingForOneAnalysis() {
        var business = business("Beauty & Personal Care");
        business.setServices("Threading, brows, lashes");
        var matches = CompetitorSelectionService.select(business, List.of(
                place("Rara Threading", "shop: beauty", 0.3),
                place("Lash Loft", "shop: beauty", 1.8)), "lash lifts");
        assertEquals("Lash Loft", matches.getFirst().place().getName());
        assertEquals("DIRECT_SPECIALTY", matches.getFirst().matchLevel());
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

    @Test
    void groceryRetailMatchesRelevantShopsButExcludesUnrelatedRetail() {
        var business = business("Retail");
        business.setName("Local Cash & Carry");
        business.setServices("Groceries and spices");
        var matches = CompetitorSelectionService.select(business, List.of(
                place("Market A", "shop: supermarket", 0.8),
                place("Convenience B", "shop: convenience", 1.0),
                place("Spice Store C", "shop: spices", 0.1),
                place("Fashion Shop D", "shop: clothes", 0.2)));
        assertEquals(3, matches.size());
        assertEquals("DIRECT_SPECIALTY", matches.get(0).matchLevel());
        assertEquals("DIRECT_SPECIALTY", matches.get(1).matchLevel());
        assertEquals("RELATED_INDUSTRY", matches.get(2).matchLevel());
        assertTrue(matches.stream().noneMatch(candidate -> candidate.place().getName().contains("Fashion")));
    }

    @Test
    void genericRetailDoesNotClaimAllNearbyStoresAreDirectCompetitors() {
        var business = business("Retail");
        business.setName("Neighborhood Shop");
        var matches = CompetitorSelectionService.select(business, List.of(
                place("General Store", "shop: general", 0.5),
                place("Dentist", "amenity: dentist", 0.2)));
        assertEquals(1, matches.size());
        assertEquals("RELATED_INDUSTRY", matches.getFirst().matchLevel());
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
