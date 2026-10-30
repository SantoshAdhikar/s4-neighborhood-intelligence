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
import com.s4.neighborhood.repository.AnalysisRunRepository;
import com.s4.neighborhood.repository.WebsiteFindingRepository;
import com.s4.neighborhood.repository.WebsitePageFindingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;

@Service
public class EvidenceSourceService {
    private final NeighborhoodResearchRepository researches;
    private final NearbyPlaceRepository places;
    private final StorefrontMediaRepository media;
    private final StorefrontSegmentationRepository segmentations;
    private final JsonMapper json;
    private final AnalysisRunRepository analysisRuns;
    private final WebsiteFindingRepository websiteFindings;
    private final WebsitePageFindingRepository pageFindings;

    public EvidenceSourceService(NeighborhoodResearchRepository researches, NearbyPlaceRepository places,
                                 StorefrontMediaRepository media, StorefrontSegmentationRepository segmentations,
                                 AnalysisRunRepository analysisRuns, WebsiteFindingRepository websiteFindings,
                                 WebsitePageFindingRepository pageFindings, JsonMapper json) {
        this.researches = researches;
        this.places = places;
        this.media = media;
        this.segmentations = segmentations;
        this.json = json;
        this.analysisRuns = analysisRuns;
        this.websiteFindings = websiteFindings;
        this.pageFindings = pageFindings;
    }

    public EvidenceBundle collect(BusinessProfile business) {
        return collect(business, true);
    }

    public EvidenceBundle collect(BusinessProfile business, boolean requireVisual) {
        List<Source> sources = new ArrayList<>();
        List<String> limitations = new ArrayList<>();
        String localProfile = "/businesses/" + business.getId() + "/edit";
        sources.add(new Source("profile-goal", "OWNER_PROFILE",
                "Owner-stated goal: " + limited(business.getPrimaryGoal(), 5000), localProfile, business.getUpdatedAt()));
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
            if (requireVisual) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Analyze a storefront image with Muse Spark before generating a report");
            }
            limitations.add("No analyzed storefront image is available. Avoid all claims about physical storefront appearance.");
        }

        var savedNeighborhood = researches.findTopByBusinessIdAndAddressKeyAndRadiusMilesOrderByFetchedAtDesc(
                business.getId(), NeighborhoodResearchService.addressKey(business),
                business.getResearchRadiusMiles());
        var latestRun = analysisRuns.findTopByBusinessIdOrderByCreatedAtDesc(business.getId());
        Set<String> shortlisted = new HashSet<>();
        latestRun.ifPresent(run -> websiteFindings.findByRunIdOrderByFetchedAtAsc(run.getId()).stream()
                .map(item -> item.getOsmSourceUrl())
                .filter(url -> url != null && !url.isBlank())
                .forEach(shortlisted::add));
        if (savedNeighborhood.isPresent()) {
            NeighborhoodResearch research = savedNeighborhood.get();
            List<NearbyPlace> nearby = places.findByResearchIdOrderByDistanceMilesAsc(research.getId());
            if (nearby.isEmpty()) {
                limitations.add("No nearby businesses were found in the saved OpenStreetMap research. Competitor coverage is unavailable, not evidence that none exist.");
            }
            List<NearbyPlace> ranked = new ArrayList<>();
            // Preserve specialty-first shortlist order in the AI source sample.
            List<String> shortlistOrder = latestRun.map(run -> websiteFindings
                    .findByRunIdOrderByFetchedAtAsc(run.getId()).stream()
                    .map(finding -> finding.getOsmSourceUrl())
                    .filter(url -> url != null && !url.isBlank())
                    .toList()).orElse(List.of());
            for (String url : shortlistOrder) {
                nearby.stream().filter(place -> url.equals(place.getSourceUrl())).forEach(ranked::add);
            }
            nearby.stream().filter(place -> !shortlisted.contains(place.getSourceUrl())).forEach(ranked::add);
            for (NearbyPlace place : ranked.stream().limit(24).toList()) {
                sources.add(new Source("osm-" + place.getOsmType() + "-" + place.getOsmId(), "OSM_PLACE",
                        "OSM listing: " + limited(place.getName(), 150) + "; category "
                                + limited(place.getCategory(), 80) + "; approximately "
                                + place.getDistanceMiles() + " miles from the researched location. "
                                + (shortlisted.contains(place.getSourceUrl())
                                ? "Selected for category-based competitor review. Specific service overlap is not independently verified. "
                                : "Nearby context only. ")
                                + (place.getWebsite() == null ? "No website recorded in this listing."
                                : "Listing contains a website URL."),
                        place.getSourceUrl(), research.getFetchedAt()));
            }
            if (shortlisted.isEmpty()) {
                limitations.add("No same-industry businesses were selected from the saved OpenStreetMap snapshot; verify its category coverage or research radius.");
            } else {
                limitations.add("Competitors were shortlisted using business categories and names. Same-category listings do not prove identical services or commercial competition.");
                if (shortlisted.size() < CompetitorSelectionService.TARGET_COUNT) {
                    limitations.add("Only " + shortlisted.size() + " potentially relevant businesses were identified in the saved search radius, fewer than the target of " + CompetitorSelectionService.TARGET_COUNT + ".");
                }
                if (shortlisted.size() > 24) {
                    limitations.add("The comparison page lists " + shortlisted.size()
                            + " OSM candidates; only the first 24 listings are included as individual AI evidence sources to keep the report bounded.");
                }
            }
        } else {
            limitations.add("No neighborhood snapshot is available for this business address and search radius. Nearby competitor claims cannot be verified; the report uses only other available evidence.");
        }
        analysisRuns.findTopByBusinessIdOrderByCreatedAtDesc(business.getId()).ifPresent(run -> {
            if (run.getFocusServices() != null && !run.getFocusServices().isBlank()) {
                sources.add(new Source("research-focus", "OWNER_PROFILE",
                        "Owner-selected competitor research focus: " + limited(run.getFocusServices(), 500)
                                + ". Name/category matches are candidate signals, not confirmed services.",
                        "/businesses/" + business.getId() + "/analyze", run.getCreatedAt()));
            }
            if (run.getCustomerFeedback() != null && !run.getCustomerFeedback().isBlank()) {
                sources.add(new Source("feedback-" + stableId(run.getCustomerFeedback()), "OWNER_FEEDBACK",
                        "Owner-supplied customer feedback notes (not independently verified): "
                                + limited(run.getCustomerFeedback(), 1000),
                        "/businesses/" + business.getId() + "/analyze", null));
                limitations.add("Customer feedback was provided by the owner, not verified against a review platform; it may not represent all customers.");
            }
            int websitesNotListed = 0;
            int websitesNotInspected = 0;
            for (var website : websiteFindings.findByRunIdOrderByFetchedAtAsc(run.getId())) {
                if ("FETCHED".equals(website.getStatus())) {
                    sources.add(new Source("website-" + stableId(website.getUrl()), website.getKind(),
                            "Public homepage metadata for " + limited(website.getBusinessName(), 90)
                                    + "; title: " + limited(website.getTitle(), 150)
                                    + "; extracted labels: " + limited(website.getObservedSummary(), 650),
                            website.getUrl(), website.getFetchedAt().truncatedTo(java.time.temporal.ChronoUnit.DAYS)));
                } else if ("NO_WEBSITE".equals(website.getStatus())) {
                    websitesNotListed++;
                } else {
                    websitesNotInspected++;
                }
            }
            var inspectedPages = pageFindings.findByRunIdOrderByFetchedAtAsc(run.getId());
            int capturedPages = 0;
            int unavailablePages = 0;
            for (var page : inspectedPages) {
                if (!"FETCHED".equals(page.getStatus())) {
                    unavailablePages++;
                    continue;
                }
                if (capturedPages >= 12) continue;
                capturedPages++;
                sources.add(new Source("page-" + page.getId(), "WEBSITE_PAGE",
                        "Observed " + limited(page.getPageType(), 25) + " page "
                                + limited(page.getPageTitle(), 90) + ": "
                                + limited(page.getObservedSummary(), 660),
                        page.getPageUrl(), page.getFetchedAt()));
            }
            if (capturedPages > 0) {
                limitations.add("Detailed website research sampled " + capturedPages
                        + " accessible internal pages; it does not establish complete service coverage, booking usability, prices, or customer experience.");
            }
            if (inspectedPages.stream().filter(page -> "FETCHED".equals(page.getStatus())).count() > 12) {
                limitations.add("Some page inspections are displayed in the research view but excluded from the AI source catalog to limit prompt size.");
            }
            if (unavailablePages > 0) {
                limitations.add(unavailablePages + " internal website pages were blocked or unavailable; absence of information is not evidence a feature is missing.");
            }
            if (websitesNotListed > 0) {
                limitations.add(websitesNotListed + " shortlisted businesses have no website recorded in OSM; this does not prove they lack websites.");
            }
            if (websitesNotInspected > 0) {
                limitations.add(websitesNotInspected + " website homepages were unavailable, disallowed or deferred; no business-quality conclusion follows from missing website evidence.");
            }
        });
        limitations.add("Website inspection samples accessible pages and labels, not complete functionality; missing links or page text do not prove a service or feature is absent. A detected booking link means it was published, not that booking works.");
        limitations.add("OpenStreetMap listings do not establish a competitor's sales, prices, quality, customer traffic, or marketing performance.");
        limitations.add("Business goals and services are owner-provided, not externally verified.");
        limitations.add("SAM masks identify candidate regions; they do not independently verify the meaning of a sign.");
        return new EvidenceBundle(List.copyOf(sources), List.copyOf(limitations));
    }

    private static String stableId(String text) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest).substring(0, 20);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static String limited(String value, int max) {
        if (value == null || value.isBlank()) return "unknown";
        String trimmed = value.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", " ").strip();
        return trimmed.substring(0, Math.min(max, trimmed.length()));
    }

    public record EvidenceBundle(List<Source> sources, List<String> limitations) {}
}
