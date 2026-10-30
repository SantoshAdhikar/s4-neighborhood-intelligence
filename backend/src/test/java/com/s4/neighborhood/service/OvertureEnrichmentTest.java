package com.s4.neighborhood.service;

import com.s4.neighborhood.entity.NearbyPlace;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OvertureEnrichmentTest {
    @Test
    void parsesOfficialPlaceFieldsAndNearbyWebsite() {
        String geoJson = """
                {"type":"FeatureCollection","features":[
                  {"type":"Feature","geometry":{"type":"Point","coordinates":[-118.1590,33.8980]},
                    "properties":{"id":"abc-123","names":{"primary":"Good Brows"},
                      "taxonomy":{"primary":"beauty_salon"},"websites":["https://goodbrows.example"],"confidence":0.96}},
                  {"type":"Feature","geometry":{"type":"Point","coordinates":[-117.0000,34.8000]},
                    "properties":{"id":"far-away","names":{"primary":"Other Shop"}}}
                ]}
                """;
        OverturePlaceCatalog catalog = new OverturePlaceCatalog(
                JsonMapper.builder().build(), "test-data", "overturemaps", false);
        var candidates = catalog.parse(geoJson, 33.898, -118.159, 4);
        assertEquals(1, candidates.size());
        assertEquals("Good Brows", candidates.getFirst().name());
        assertEquals("https://goodbrows.example", candidates.getFirst().website());
    }

    @Test
    void enrichesMissingOsmWebsiteWithoutInventingDuplicateBusiness() {
        NearbyPlace osm = new NearbyPlace();
        osm.setOsmType("node");
        osm.setOsmId(12345);
        osm.setName("Good Brows");
        osm.setCategory("shop beauty");
        osm.setLatitude(33.8980);
        osm.setLongitude(-118.1590);
        osm.setDistanceMiles(0.7);
        osm.setSourceUrl("https://www.openstreetmap.org/node/12345");

        var overture = List.of(
                new OverturePlaceCatalog.Place("abc-123", "Good Brows", "beauty_salon",
                        33.89803, -118.15902, 0.7, "https://goodbrows.example"),
                new OverturePlaceCatalog.Place("abc-999", "Different Lash Studio", "beauty_salon",
                        33.90000, -118.15700, 0.9, "https://lash.example")
        );
        var merged = WebsiteResearchService.combinePlaces(List.of(osm), overture);
        assertEquals(2, merged.places().size());
        assertEquals("https://goodbrows.example", merged.places().getFirst().getWebsite());
        assertEquals("node", merged.places().getFirst().getOsmType());
        assertEquals("overture", merged.places().get(1).getOsmType());
        assertEquals("abc-123", merged.overtureIds().get(osm.getSourceUrl()));
    }

    @Test
    void neverMergesSimilarNamesFromDifferentLocations() {
        NearbyPlace osm = new NearbyPlace();
        osm.setOsmType("node");
        osm.setOsmId(456);
        osm.setName("Good Brows");
        osm.setLatitude(33.898);
        osm.setLongitude(-118.159);
        osm.setSourceUrl("https://www.openstreetmap.org/node/456");
        var far = new OverturePlaceCatalog.Place("far-id", "Good Brows", "beauty_salon",
                33.95, -118.25, 5.5, "https://not-same.example");
        var merged = WebsiteResearchService.combinePlaces(List.of(osm), List.of(far));
        assertNull(merged.places().getFirst().getWebsite());
        assertFalse(merged.overtureIds().containsKey(osm.getSourceUrl()));
    }
}
