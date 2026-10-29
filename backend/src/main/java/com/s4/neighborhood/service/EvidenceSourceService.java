package com.s4.neighborhood.service;

import com.s4.neighborhood.dto.EvidenceReportResponse.Source;
import com.s4.neighborhood.dto.VisualFindings;
import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.entity.NearbyPlace;
import com.s4.neighborhood.entity.NeighborhoodResearch;
import com.s4.neighborhood.entity.StorefrontMedia;
import com.s4.neighborhood.repository.NearbyPlaceRepository;
import com.s4.neighborhood.repository.NeighborhoodResearchRepository;
import com.s4.neighborhood.repository.StorefrontMediaRepository;
import com.s4.neighborhood.repository.StorefrontSegmentationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class EvidenceSourceService {
    private final NeighborhoodResearchRepository researches;
    private final NearbyPlaceRepository places;
    private final StorefrontMediaRepository media;
    private final StorefrontSegmentationRepository segmentations;
    private final JsonMapper json;

    public EvidenceSourceService(NeighborhoodResearchRepository researches, NearbyPlaceRepository places,
                                 StorefrontMediaRepository media, StorefrontSegmentationRepository segmentations,
                                 JsonMapper json) {
        this.researches = researches;
        this.places = places;
        this.media = media;
        this.segmentations = segmentations;
        this.json = json;
    }

    public EvidenceBundle collect(BusinessProfile business) {
        List<Source> sources = new ArrayList<>();
        List<String> limitations = new ArrayList<>();
        String localProfile = "/businesses/" + business.getId() + "/edit";
        sources.add(new Source("profile-goal", "OWNER_PROFILE",
                "Owner-stated goal: " + limited(business.getPrimaryGoal(), 400), localProfile, business.getUpdatedAt()));
        sources.add(new Source("profile-services", "OWNER_PROFILE",
                "Owner-reported services: " + limited(business.getServices(), 400), localProfile, business.getUpdatedAt()));
        sources.add(new Source("profile-location", "OWNER_PROFILE",
                "Owner-reported location: " + limited(String.join(", ", business.getStreetAddress(),
                        business.getCity(), business.getState(), business.getPostalCode()), 400),
                localProfile, business.getUpdatedAt()));

        int observed = 0;
        for (StorefrontMedia item : media.findByBusinessIdOrderByCreatedAtDesc(business.getId())) {
            if (!"ANALYZED".equals(item.getStatus()) || item.getAnalysisJson() == null) continue;
            if (observed >= 3) break;
            observed++;
            VisualFindings findings;
            try {
                findings = json.readValue(item.getAnalysisJson(), VisualFindings.class);
            } catch (RuntimeException exception) {
                limitations.add("One saved storefront analysis could not be read.");
                continue;
            }
            if (findings.observations() == null) continue;
            int index = 0;
            for (VisualFindings.Observation observation : findings.observations()) {
                if (index >= 8) break;
                index++;
                sources.add(new Source("visual-" + item.getId() + "-" + index, "STORE_FRONT_IMAGE",
                        "Muse observation (" + limited(observation.certainty(), 20) + ") from "
                                + limited(item.getOriginalFilename(), 90) + ": "
                                + limited(observation.aspect(), 80) + ": "
                                + limited(observation.visibleEvidence(), 360),
                        "/businesses/" + business.getId() + "/storefront", item.getAnalyzedAt()));
            }
            if (findings.limitations() != null) {
                for (String limit : findings.limitations().stream().limit(3).toList()) {
                    limitations.add("Storefront limitation: " + limited(limit, 220));
                }
            }
            segmentations.findByMediaIdOrderByCreatedAtDesc(item.getId()).stream()
                    .filter(result -> "COMPLETED".equals(result.getStatus()))
                    .limit(3)
                    .forEach(result -> sources.add(new Source("sam-" + result.getId(), "SAM_SEGMENTATION",
                            "SAM created a mask for requested concept '" + limited(result.getConcept(), 80)
                                    + "' on " + limited(item.getOriginalFilename(), 90)
                                    + ". Mask matching and semantic correctness require visual review.",
                            "/businesses/" + business.getId() + "/storefront", result.getCompletedAt())));
        }
        if (sources.stream().noneMatch(source -> "STORE_FRONT_IMAGE".equals(source.kind()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Analyze a storefront image with Muse Spark before generating a report");
        }

        NeighborhoodResearch research = researches.findTopByBusinessIdOrderByFetchedAtDesc(business.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Run Neighborhood Explorer before generating a report"));
        String currentKey = String.join("|", business.getStreetAddress(), business.getCity(), business.getState(),
                business.getPostalCode(), business.getIndustry()).strip().toLowerCase(Locale.ROOT);
        if (!currentKey.equals(research.getAddressKey()) || business.getResearchRadiusMiles() != research.getRadiusMiles()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The business location or research radius changed. Rerun Neighborhood Explorer first");
        }
        List<NearbyPlace> nearby = places.findByResearchIdOrderByDistanceMilesAsc(research.getId());
        if (nearby.isEmpty()) limitations.add("No nearby businesses were found in the saved OpenStreetMap research.");
        for (NearbyPlace place : nearby.stream().limit(20).toList()) {
            sources.add(new Source("osm-" + place.getOsmType() + "-" + place.getOsmId(), "OSM_PLACE",
                    "OSM listing: " + limited(place.getName(), 150) + "; category "
                            + limited(place.getCategory(), 80) + "; approximately "
                            + place.getDistanceMiles() + " miles from the researched location. "
                            + (place.getWebsite() == null ? "No website recorded in this listing."
                            : "Listing contains a website URL."),
                    place.getSourceUrl(), research.getFetchedAt()));
        }
        limitations.add("OpenStreetMap listings do not establish a competitor's sales, prices, quality, customer traffic, or marketing performance.");
        limitations.add("Business goals and services are owner-provided, not externally verified.");
        limitations.add("SAM masks identify candidate regions; they do not independently verify the meaning of a sign.");
        return new EvidenceBundle(List.copyOf(sources), List.copyOf(limitations));
    }

    private static String limited(String value, int max) {
        if (value == null || value.isBlank()) return "unknown";
        String trimmed = value.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", " ").strip();
        return trimmed.substring(0, Math.min(max, trimmed.length()));
    }

    public record EvidenceBundle(List<Source> sources, List<String> limitations) {}
}
