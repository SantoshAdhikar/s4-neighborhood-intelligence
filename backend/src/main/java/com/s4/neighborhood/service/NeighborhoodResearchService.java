
package com.s4.neighborhood.service;

import com.s4.neighborhood.client.OpenStreetMapClient;
import com.s4.neighborhood.dto.NeighborhoodResponse;
import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.entity.NearbyPlace;
import com.s4.neighborhood.entity.NeighborhoodResearch;
import com.s4.neighborhood.repository.BusinessProfileRepository;
import com.s4.neighborhood.repository.NearbyPlaceRepository;
import com.s4.neighborhood.repository.NeighborhoodResearchRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class NeighborhoodResearchService {
    private static final Duration CACHE_AGE = Duration.ofHours(24);
    private static final int MAX_DISPLAYED = 400;

    private final BusinessProfileRepository businesses;
    private final NeighborhoodResearchRepository researches;
    private final NearbyPlaceRepository places;
    private final OpenStreetMapClient osm;

    public NeighborhoodResearchService(
            BusinessProfileRepository businesses,
            NeighborhoodResearchRepository researches,
            NearbyPlaceRepository places,
            OpenStreetMapClient osm
    ) {
        this.businesses = businesses;
        this.researches = researches;
        this.places = places;
        this.osm = osm;
    }

    /** Accept only saved neighborhood research for this business, address and radius. */
    public boolean hasSavedSnapshot(UUID businessId) {
        BusinessProfile business = businesses.findById(businessId)
                .orElseThrow(() -> new NoSuchElementException("Business not found: " + businessId));
        return researches.findTopByBusinessIdAndAddressKeyAndRadiusMilesOrderByFetchedAtDesc(
                businessId, addressKey(business), business.getResearchRadiusMiles()).isPresent();
    }

    static String addressKey(BusinessProfile business) {
        return String.join("|", business.getStreetAddress(), business.getCity(),
                        business.getState(), business.getPostalCode(), business.getIndustry())
                .strip().toLowerCase(Locale.ROOT)
                // The V9 retail search used the generic shop filter. Rebuild old
                // retail snapshots once so the new grocery/retail taxonomy applies.
                + ("Retail".equalsIgnoreCase(business.getIndustry()) ? "|retail-taxonomy-v2" : "");
    }

    public NeighborhoodResponse research(UUID businessId) {
        BusinessProfile business = businesses.findById(businessId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Business not found: " + businessId));

        String addressKey = addressKey(business);

        int radius = business.getResearchRadiusMiles();

        Optional<NeighborhoodResearch> cached = researches
                .findTopByBusinessIdAndAddressKeyAndRadiusMilesOrderByFetchedAtDesc(
                        businessId, addressKey, radius)
                .filter(snapshot -> snapshot.getFetchedAt()
                        .isAfter(Instant.now().minus(CACHE_AGE)));

        if (cached.isPresent()) {
            return response(business, cached.get(), true);
        }

        OpenStreetMapClient.Location location = osm.geocode(business);

        List<NearbyPlace> nearby = osm
                .findNearby(location, radius, business.getIndustry())
                .stream()
                .filter(place -> !place.name().equalsIgnoreCase(
                        business.getName()))
                .filter(place -> distanceMiles(
                        location.latitude(),
                        location.longitude(),
                        place.latitude(),
                        place.longitude()) <= radius)
                .sorted(Comparator.comparingDouble(place -> distanceMiles(
                        location.latitude(),
                        location.longitude(),
                        place.latitude(),
                        place.longitude())))
                .limit(MAX_DISPLAYED)
                .map(place -> toEntity(place, location))
                .toList();

        NeighborhoodResearch snapshot = new NeighborhoodResearch();
        snapshot.setBusinessId(businessId);
        snapshot.setAddressKey(addressKey);
        snapshot.setRadiusMiles(radius);
        snapshot.setCenterLatitude(location.latitude());
        snapshot.setCenterLongitude(location.longitude());
        snapshot.setMatchedAddress(location.label());
        snapshot.setFetchedAt(Instant.now());

        snapshot = researches.saveAndFlush(snapshot);

        for (NearbyPlace place : nearby) {
            place.setResearchId(snapshot.getId());
        }

        places.saveAll(nearby);

        return response(business, snapshot, false);
    }

    private NearbyPlace toEntity(
            OpenStreetMapClient.OsmPlace place,
            OpenStreetMapClient.Location center
    ) {
        NearbyPlace item = new NearbyPlace();

        item.setOsmType(place.type());
        item.setOsmId(place.id());
        item.setName(place.name().substring(
                0, Math.min(place.name().length(), 250)));
        item.setCategory(place.category());
        item.setLatitude(place.latitude());
        item.setLongitude(place.longitude());

        item.setDistanceMiles(Math.round(distanceMiles(
                center.latitude(),
                center.longitude(),
                place.latitude(),
                place.longitude()) * 100.0) / 100.0);

        item.setWebsite(place.website().isBlank()
                ? null
                : place.website().substring(
                0, Math.min(place.website().length(), 500)));

        item.setSourceUrl("https://www.openstreetmap.org/"
                + place.type() + "/" + place.id());

        return item;
    }

    private NeighborhoodResponse response(
            BusinessProfile business,
            NeighborhoodResearch research,
            boolean cached
    ) {
        var found = places
                .findByResearchIdOrderByDistanceMilesAsc(research.getId())
                .stream()
                .map(place -> new NeighborhoodResponse.NearbyBusinessResponse(
                        place.getOsmType(),
                        place.getOsmId(),
                        place.getName(),
                        place.getCategory(),
                        place.getLatitude(),
                        place.getLongitude(),
                        place.getDistanceMiles(),
                        place.getWebsite(),
                        place.getSourceUrl()))
                .toList();

        return new NeighborhoodResponse(
                business.getId(),
                business.getName(),
                research.getRadiusMiles(),
                research.getCenterLatitude(),
                research.getCenterLongitude(),
                research.getMatchedAddress(),
                research.getFetchedAt(),
                cached,
                found);
    }

    static double distanceMiles(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {
        double a = Math.pow(
                Math.sin(Math.toRadians(lat2 - lat1) / 2), 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(Math.toRadians(lon2 - lon1) / 2), 2);

        return 3958.7613 * 2
                * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
