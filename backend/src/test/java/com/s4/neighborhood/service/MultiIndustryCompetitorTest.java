package com.s4.neighborhood.service;

import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.entity.NearbyPlace;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MultiIndustryCompetitorTest {
    @Test void hairSalonBeatsNailSpaAndBarberHasSpecialtySignal() {
        var owner = owner("Beauty & Personal Care", "Hair salon", "Haircuts, coloring, styling");
        var result = CompetitorSelectionService.select(owner, List.of(
                place("Nail Palace", "shop: beauty", 0.2),
                place("Hair by Ana", "shop: hairdresser", 1.4),
                place("City Barbers", "shop: hairdresser", 1.0)));
        assertEquals("DIRECT_SPECIALTY", result.getFirst().matchLevel());
        assertEquals("RELATED_INDUSTRY", result.getLast().matchLevel());
    }

    @Test void threadingStudioPrefersThreadingShops() {
        var owner = owner("Beauty & Personal Care", "Golden Brows", "eyebrow threading, brow lamination, lash lifts");
        var result = CompetitorSelectionService.select(owner, List.of(
                place("The Nail Spa", "shop: beauty", 0.1),
                place("Rara Threading", "shop: beauty", 2.2),
                place("Rewa Threading", "shop: beauty", 1.2)));
        assertEquals("Rewa Threading", result.get(0).place().getName());
        assertEquals("Rara Threading", result.get(1).place().getName());
    }

    @Test void nepaliRestaurantUsesCuisineTagAndNotPharmacy() {
        var owner = owner("Restaurant & Food", "Everest Kitchen", "Nepali cuisine, momos and thali");
        var result = CompetitorSelectionService.select(owner, List.of(
                place("Nepal Restaurant", "amenity: restaurant; cuisine: nepalese", 2.0),
                place("Generic Restaurant", "amenity: restaurant", 0.1),
                place("Pharmacy", "amenity: pharmacy", 0.05)));
        assertEquals(2, result.size());
        assertEquals("DIRECT_SPECIALTY", result.getFirst().matchLevel());
        assertEquals("POTENTIAL_OVERLAP", result.getLast().matchLevel());
    }

    @Test void autoMechanicGetsRepairShopsInsteadOfDealers() {
        var owner = owner("Automotive", "Reliable Auto", "Brake repair, engine diagnostics and maintenance");
        var result = CompetitorSelectionService.select(owner, List.of(
                place("Fast Car Repairs", "shop: car_repair", 1.8),
                place("Used Cars", "shop: car", 0.5),
                place("Barbershop", "shop: hairdresser", 0.2)));
        assertEquals(2, result.size());
        assertEquals("Fast Car Repairs", result.getFirst().place().getName());
    }

    @Test void schoolAndPetGroomersUseTheirOwnSpecialties() {
        var tutor = owner("Education & Tutoring", "My Tutoring", "Math tutoring");
        var vet = owner("Pet Services", "Dog Spa", "Pet grooming");
        assertEquals("DIRECT_SPECIALTY", CompetitorSelectionService.select(tutor,
                List.of(place("Math Tutors", "amenity: school", 1))).getFirst().matchLevel());
        assertEquals("DIRECT_SPECIALTY", CompetitorSelectionService.select(vet,
                List.of(place("Dog Groomers", "shop: pet_grooming", 1))).getFirst().matchLevel());
    }

    @Test void differentBusinessProfilesDoNotLeakIndustryRules() {
        var locations = List.of(place("Pizza Palace", "amenity: restaurant", 1),
                place("Dental Care", "amenity: dentist", 0.5));
        assertEquals("Pizza Palace", CompetitorSelectionService.select(owner("Restaurant & Food", "Pizza Shop", "pizza"), locations).getFirst().place().getName());
        assertEquals("Dental Care", CompetitorSelectionService.select(owner("Health & Wellness", "My Dentist", "dental care"), locations).getFirst().place().getName());
    }

    private static BusinessProfile owner(String industry, String name, String services) {
        var business = new BusinessProfile(); business.setName(name); business.setIndustry(industry); business.setServices(services); return business;
    }
    private static NearbyPlace place(String name, String category, double distance) {
        var place = new NearbyPlace(); place.setName(name); place.setCategory(category); place.setDistanceMiles(distance); return place;
    }
}
