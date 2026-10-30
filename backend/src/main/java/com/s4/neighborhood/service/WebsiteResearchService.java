package com.s4.neighborhood.service;

import com.s4.neighborhood.entity.AnalysisRun;
import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.entity.NearbyPlace;
import com.s4.neighborhood.entity.WebsiteFinding;
import com.s4.neighborhood.entity.WebsitePageFinding;
import com.s4.neighborhood.repository.NearbyPlaceRepository;
import com.s4.neighborhood.repository.NeighborhoodResearchRepository;
import com.s4.neighborhood.repository.WebsiteFindingRepository;
import com.s4.neighborhood.repository.WebsitePageFindingRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Service
public class WebsiteResearchService {
    private static final int MAX_HTML_BYTES = 1_048_576;
    private static final int MAX_COMPETITOR_HOMEPAGES = 12;
    private static final int MAX_DEEP_COMPETITORS = 5;
    private static final int MAX_COMPETITOR_DETAIL_PAGES = 2;
    private static final int MAX_OWNER_DETAIL_PAGES = 4;
    private static final String USER_AGENT = "S4NeighborhoodIntelligence/0.1 (+https://github.com/SantoshAdhikar/s4-neighborhood-intelligence)";
    private final WebsiteFindingRepository findings;
    private final WebsitePageFindingRepository pages;
    private final NeighborhoodResearchRepository researches;
    private final NearbyPlaceRepository places;
    private final OverturePlaceCatalog overture;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public WebsiteResearchService(WebsiteFindingRepository findings, WebsitePageFindingRepository pages,
                                  NeighborhoodResearchRepository researches, NearbyPlaceRepository places,
                                  OverturePlaceCatalog overture) {
        this.findings = findings;
        this.pages = pages;
        this.researches = researches;
        this.places = places;
        this.overture = overture;
    }

    // Enrich OSM candidates with local Overture records; inspect only a bounded set of websites.
    public void research(AnalysisRun run, BusinessProfile business) {
        if (business.getWebsite() != null && !business.getWebsite().isBlank()) {
            WebsiteFinding owner = newFinding(run, "OWNER_WEBSITE", business.getName(), business.getWebsite());
            Document homepage = inspectHomepage(owner, business.getWebsite());
            findings.saveAndFlush(owner);
            inspectDetails(owner, homepage, MAX_OWNER_DETAIL_PAGES);
        }
        var latest = researches.findTopByBusinessIdAndAddressKeyAndRadiusMilesOrderByFetchedAtDesc(
                business.getId(), NeighborhoodResearchService.addressKey(business),
                business.getResearchRadiusMiles());
        if (latest.isEmpty()) return;
        java.util.Set<String> seenHosts = new java.util.HashSet<>();
        if (business.getWebsite() != null) seenHosts.add(hostname(business.getWebsite()));

        var existing = places.findByResearchIdOrderByDistanceMilesAsc(latest.get().getId());
        var additions = overture.nearby(latest.get().getCenterLatitude(),
                latest.get().getCenterLongitude(), business.getResearchRadiusMiles());
        var enriched = combinePlaces(existing, additions);
        var matches = CompetitorSelectionService.select(business, enriched.places(), run.getFocusServices());
        int inspectedHomepages = 0;
        int researchedInDepth = 0;
        for (var match : matches) {
            NearbyPlace place = match.place();
            String site = place.getWebsite();
            WebsiteFinding finding = newFinding(run, "NEARBY_COMPETITOR", place.getName(), place.getSourceUrl());
            boolean overtureListing = "overture".equals(place.getOsmType());
            finding.setOsmSourceUrl(overtureListing ? null : place.getSourceUrl());
            String overtureId = enriched.overtureIds().get(place.getSourceUrl());
            String provenance = overtureId == null ? "" :
                    "Overture Maps record " + overtureId + "; website attribution is a candidate, not verified ownership.";
            finding.setCategory(limit(place.getCategory(), 150));
            finding.setDistanceMiles(place.getDistanceMiles());
            finding.setMatchLevel(match.matchLevel());
            finding.setNote(limit(match.reason(), 350));
            Document homepage = null;

            if (site == null || site.isBlank()) {
                finding.setStatus("NO_WEBSITE");
                finding.setNote(overtureListing
                        ? "No website in this Overture record; this does not prove the business lacks a website."
                        : "No website listed in OSM; this does not prove the business lacks a website.");
            } else {
                String host = hostname(site);
                if (host.isBlank() || !seenHosts.add(host)) {
                    finding.setStatus("SKIPPED");
                    finding.setNote("Website not inspected: invalid or duplicate host.");
                } else if (inspectedHomepages >= MAX_COMPETITOR_HOMEPAGES) {
                    finding.setStatus("SKIPPED");
                    finding.setNote("Listing retained; homepage inspection deferred to limit public requests.");
                } else {
                    inspectedHomepages++;
                    finding.setUrl(limit(site, 500));
                    homepage = inspectHomepage(finding, site);
                }
            }
            String classification = match.reason();
            String researchNote = finding.getNote() == null ? "" : finding.getNote();
            finding.setNote(limit(classification + (provenance.isBlank() ? "" : " " + provenance)
                    + (researchNote.isBlank() || classification.equals(researchNote) ? "" : " " + researchNote), 350));
            findings.saveAndFlush(finding);
            if (homepage != null && researchedInDepth < MAX_DEEP_COMPETITORS) {
                inspectDetails(finding, homepage, MAX_COMPETITOR_DETAIL_PAGES);
                researchedInDepth++;
            }
        }
    }


    static CombinedPlaces combinePlaces(List<NearbyPlace> existing,
                                        List<OverturePlaceCatalog.Place> overturePlaces) {
        List<NearbyPlace> combined = new java.util.ArrayList<>();
        java.util.Map<String, String> overtureIds = new java.util.HashMap<>();
        java.util.Set<String> presentNames = new java.util.HashSet<>();
        java.util.Set<String> matchedIds = new java.util.HashSet<>();
        for (NearbyPlace osmPlace : existing) {
            presentNames.add(normalizedName(osmPlace.getName()));
            var matching = overturePlaces.stream()
                    .filter(candidate -> namesMatch(osmPlace.getName(), candidate.name()) &&
                            OverturePlaceCatalog.distanceMiles(osmPlace.getLatitude(), osmPlace.getLongitude(),
                                    candidate.latitude(), candidate.longitude()) <= 0.075)
                    .findFirst();
            matching.ifPresent(found -> matchedIds.add(found.id()));
            if (matching.isPresent() && (osmPlace.getWebsite() == null || osmPlace.getWebsite().isBlank()) &&
                    !matching.get().website().isBlank()) {
                NearbyPlace enriched = copyPlace(osmPlace);
                enriched.setWebsite(matching.get().website());
                combined.add(enriched);
                overtureIds.put(enriched.getSourceUrl(), matching.get().id());
            } else {
                combined.add(osmPlace);
            }
        }
        for (var place : overturePlaces) {
            if (matchedIds.contains(place.id())) continue;
            String name = normalizedName(place.name());
            if (name.isBlank() || !presentNames.add(name)) continue;
            NearbyPlace candidate = new NearbyPlace();
            candidate.setOsmType("overture");
            candidate.setOsmId(Integer.toUnsignedLong(place.id().hashCode()));
            candidate.setName(limit(place.name(), 250));
            candidate.setCategory(overtureCategory(place.category()));
            candidate.setLatitude(place.latitude());
            candidate.setLongitude(place.longitude());
            candidate.setDistanceMiles(Math.round(place.distanceMiles() * 100.0) / 100.0);
            candidate.setWebsite(place.website().isBlank() ? null : place.website());
            candidate.setSourceUrl("overture:" + place.id());
            combined.add(candidate);
            overtureIds.put(candidate.getSourceUrl(), place.id());
        }
        return new CombinedPlaces(combined, overtureIds);
    }

    private static NearbyPlace copyPlace(NearbyPlace original) {
        NearbyPlace copy = new NearbyPlace();
        copy.setOsmType(original.getOsmType());
        copy.setOsmId(original.getOsmId());
        copy.setName(original.getName());
        copy.setCategory(original.getCategory());
        copy.setLatitude(original.getLatitude());
        copy.setLongitude(original.getLongitude());
        copy.setDistanceMiles(original.getDistanceMiles());
        copy.setWebsite(original.getWebsite());
        copy.setSourceUrl(original.getSourceUrl());
        return copy;
    }

    static boolean namesMatch(String first, String second) {
        String a = normalizedName(first);
        String b = normalizedName(second);
        if (a.equals(b) && !a.isBlank()) return true;
        java.util.Set<String> aTokens = new java.util.HashSet<>(List.of(a.split(" ")));
        java.util.Set<String> bTokens = new java.util.HashSet<>(List.of(b.split(" ")));
        if (aTokens.size() < 2 || bTokens.size() < 2) return false;
        int intersection = 0;
        for (String token : aTokens) if (bTokens.contains(token)) intersection++;
        return intersection >= 2 && (double) intersection / (aTokens.size() + bTokens.size() - intersection) >= 0.85;
    }

    private static String normalizedName(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
    }

    private static String overtureCategory(String category) {
        String value = category == null ? "" : category.toLowerCase(Locale.ROOT).replace('_', ' ');
        if (value.contains("beauty") || value.contains("cosmetic") || value.contains("eyebrow") || value.contains("eyelash"))
            return "shop beauty (Overture: " + limit(value, 95) + ")";
        if (value.contains("nail") || value.contains("hair") || value.contains("barber") || value.contains("spa"))
            return "shop hairdresser (Overture: " + limit(value, 90) + ")";
        if (value.contains("grocery") || value.contains("supermarket") || value.contains("convenience"))
            return "shop supermarket (Overture: " + limit(value, 85) + ")";
        if (value.contains("restaurant") || value.contains("cafe"))
            return "amenity restaurant (Overture: " + limit(value, 90) + ")";
        return "Overture: " + limit(value, 135);
    }

    record CombinedPlaces(List<NearbyPlace> places, java.util.Map<String, String> overtureIds) {}

    private WebsiteFinding newFinding(AnalysisRun run, String kind, String name, String address) {
        WebsiteFinding finding = new WebsiteFinding();
        finding.setRunId(run.getId());
        finding.setBusinessId(run.getBusinessId());
        finding.setKind(kind);
        finding.setBusinessName(limit(name, 160));
        finding.setUrl(limit(address, 500));
        return finding;
    }

    private Document inspectHomepage(WebsiteFinding finding, String address) {
        try {
            URI uri = checkedUri(address);
            if (!allowedByRobots(uri)) {
                finding.setStatus("BLOCKED");
                finding.setNote("Website policy disallows automated inspection.");
                return null;
            }
            HttpResponse<InputStream> response = request(uri);
            try (InputStream stream = response.body()) {
                if (response.statusCode() != 200 || !isHtml(response)) {
                    finding.setStatus("UNAVAILABLE");
                    finding.setNote("Homepage did not return accessible HTML (HTTP " + response.statusCode() + ").");
                    return null;
                }
                byte[] bytes = stream.readNBytes(MAX_HTML_BYTES + 1);
                boolean truncated = bytes.length > MAX_HTML_BYTES;
                String html = new String(bytes, 0, Math.min(bytes.length, MAX_HTML_BYTES),
                        java.nio.charset.StandardCharsets.UTF_8);
                Document document = Jsoup.parse(html, response.uri().toString());
                finding.setStatus("FETCHED");
                finding.setUrl(limit(response.uri().toString(), 500));
                finding.setTitle(limit(document.title(), 250));
                finding.setObservedSummary(extract(document));
                finding.setNote(truncated
                        ? "Partial homepage: first 1 MiB inspected. Additional content may be missing."
                        : "Homepage content and visible links extracted, not a full service or usability audit.");
                return document;
            }
        } catch (Exception exception) {
            finding.setStatus("UNAVAILABLE");
            finding.setNote("Homepage could not be inspected safely. No conclusion about business quality.");
            return null;
        }
    }

    private void inspectDetails(WebsiteFinding website, Document homepage, int maximum) {
        if (homepage == null) return;
        for (PageCandidate candidate : detailLinks(homepage, maximum)) {
            WebsitePageFinding page = new WebsitePageFinding();
            page.setRunId(website.getRunId());
            page.setBusinessId(website.getBusinessId());
            page.setWebsiteFindingId(website.getId());
            page.setPageType(candidate.type());
            page.setPageUrl(candidate.url());
            try {
                URI uri = checkedUri(candidate.url());
                if (!allowedByRobots(uri)) {
                    page.setStatus("BLOCKED");
                    page.setNote("Page is disallowed by website policy.");
                } else {
                    HttpResponse<InputStream> response = request(uri);
                    try (InputStream stream = response.body()) {
                        if (response.statusCode() != 200 || !isHtml(response)) {
                            page.setStatus("UNAVAILABLE");
                            page.setNote("Page did not return accessible HTML (HTTP " + response.statusCode() + ").");
                        } else {
                            byte[] bytes = stream.readNBytes(MAX_HTML_BYTES + 1);
                            boolean truncated = bytes.length > MAX_HTML_BYTES;
                            Document document = Jsoup.parse(new String(bytes, 0,
                                    Math.min(bytes.length, MAX_HTML_BYTES),
                                    java.nio.charset.StandardCharsets.UTF_8), response.uri().toString());
                            page.setStatus("FETCHED");
                            page.setPageUrl(limit(response.uri().toString(), 500));
                            page.setPageTitle(limit(document.title(), 250));
                            page.setObservedSummary(extractDetail(document));
                            page.setNote(truncated ? "Partial page: only first 1 MiB inspected." :
                                    "Observed page text; availability and details may change.");
                        }
                    }
                }
            } catch (Exception exception) {
                page.setStatus("UNAVAILABLE");
                page.setNote("Page inaccessible or failed safety checks; absence is not established.");
            }
            pages.saveAndFlush(page);
        }
    }

    static List<PageCandidate> detailLinks(Document homepage, int maxPages) {
        URI root = URI.create(homepage.baseUri());
        String hostname = normalizedHost(root.getHost());
        java.util.Map<String, PageCandidate> byType = new java.util.LinkedHashMap<>();
        for (Element anchor : homepage.select("a[href]")) {
            String value = anchor.attr("abs:href");
            try {
                URI uri = URI.create(value).normalize();
                if (!"https".equalsIgnoreCase(uri.getScheme()) ||
                        !hostname.equals(normalizedHost(uri.getHost())) || uri.getPort() != -1 && uri.getPort() != 443 ||
                        uri.getUserInfo() != null) continue;
                String path = uri.getPath();
                if (path == null || path.isBlank() || path.equals("/") ||
                        path.matches("(?i).*[.](?:pdf|png|jpe?g|webp|svg|mp4|zip|css|js)$") ||
                        path.matches("(?i).*(?:/login|/sign-in|/cart|/checkout|/account).*")) continue;
                String type = pageType(path + " " + anchor.text());
                if (type == null || byType.containsKey(type)) continue;
                URI clean = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null);
                if (clean.toString().length() > 500 || clean.equals(root)) continue;
                byType.put(type, new PageCandidate(type, clean.toString()));
            } catch (Exception ignored) {
                // Ignore invalid or unsafe navigation targets; never guess their destination.
            }
        }
        return java.util.stream.Stream.of("SERVICES", "BOOKING", "PRICING", "CONTACT", "ABOUT")
                .map(byType::get).filter(java.util.Objects::nonNull).limit(maxPages).toList();
    }

    private static String pageType(String text) {
        String value = text.toLowerCase(Locale.ROOT);
        if (value.matches(".*(?:book|appoint|reserv|schedule).*")) return "BOOKING";
        if (value.matches(".*(?:pric|rate|cost|special|offer|promotion).*")) return "PRICING";
        if (value.matches(".*(?:service|treatment|menu|brow|lash|thread|wax|product|catalog|collection).*")) return "SERVICES";
        if (value.matches(".*(?:contact|location|visit|hours|directions).*")) return "CONTACT";
        if (value.matches(".*(?:about|our.story|team).*")) return "ABOUT";
        return null;
    }

    private static boolean isHtml(HttpResponse<?> response) {
        return response.headers().firstValue("Content-Type").orElse("")
                .toLowerCase(Locale.ROOT).contains("text/html");
    }

    public record PageCandidate(String type, String url) {}

    private static String normalizedHost(String host) {
        return host == null ? "" : host.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
    }

    private HttpResponse<InputStream> request(URI uri) throws Exception {
        for (int redirects = 0; redirects <= 2; redirects++) {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html")
                    .GET().build();
            HttpResponse<InputStream> result = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (List.of(301, 302, 303, 307, 308).contains(result.statusCode()) && redirects < 2) {
                result.body().close();
                String redirect = result.headers().firstValue("Location").orElseThrow();
                URI next = checkedUri(uri.resolve(redirect).toString());
                if (!next.getHost().replaceFirst("(?i)^www\\.", "").equalsIgnoreCase(
                        uri.getHost().replaceFirst("(?i)^www\\.", ""))) {
                    throw new IllegalArgumentException("Cross-domain redirect requires separate review");
                }
                if (!allowedByRobots(next)) throw new IllegalArgumentException("Disallowed by robots");
                uri = next;
                continue;
            }
            return result;
        }
        throw new IllegalStateException("Redirect limit reached");
    }

    private boolean allowedByRobots(URI uri) throws Exception {
        URI robots = checkedUri("https://" + uri.getHost() + "/robots.txt");
        HttpRequest request = HttpRequest.newBuilder(robots)
                .timeout(Duration.ofSeconds(7)).header("User-Agent", USER_AGENT)
                .GET().build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream stream = response.body()) {
            if (response.statusCode() == 404 || response.statusCode() == 410) return true;
            if (response.statusCode() != 200) return false;
            byte[] bytes = stream.readNBytes(32_001);
            if (bytes.length > 32_000) return false;
            String[] lines = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).split("\n");
            boolean matching = false;
            for (String line : lines) {
                String text = line.split("#", 2)[0].strip();
                String lower = text.toLowerCase(Locale.ROOT);
                if (lower.startsWith("user-agent:")) {
                    String agent = lower.substring(11).strip();
                    matching = agent.equals("*") || agent.equals("s4neighborhoodresearch");
                } else if (matching && lower.startsWith("disallow:")) {
                    String path = text.substring(9).strip();
                    if (!path.isEmpty() && (uri.getPath().isBlank() ? "/" : uri.getPath()).startsWith(path.replace("*", ""))) return false;
                }
            }
            return true;
        }
    }

    static URI checkedUri(String address) throws Exception {
        URI uri = URI.create(address.strip()).normalize();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null ||
                uri.getPort() != -1 && uri.getPort() != 443 || uri.getHost() == null ||
                !uri.getHost().contains(".") || uri.getHost().length() > 253) {
            throw new IllegalArgumentException("Only public HTTPS website addresses are supported");
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.endsWith(".local") || host.endsWith(".internal") || host.endsWith(".test") ||
                host.endsWith(".localhost")) throw new IllegalArgumentException("Not public");
        for (InetAddress resolved : InetAddress.getAllByName(host)) {
            if (resolved.isAnyLocalAddress() || resolved.isLoopbackAddress() ||
                    resolved.isSiteLocalAddress() || resolved.isLinkLocalAddress() ||
                    resolved.isMulticastAddress() || privateRange(resolved.getAddress())) {
                throw new IllegalArgumentException("Private network address blocked");
            }
        }
        return uri;
    }

    private static boolean privateRange(byte[] bytes) {
        if (bytes.length == 4) {
            int first = bytes[0] & 255, second = bytes[1] & 255;
            return first == 0 || first == 10 || first == 127 || first >= 224 ||
                    first == 169 && second == 254 || first == 172 && second >= 16 && second <= 31 ||
                    first == 192 && second == 168 || first == 100 && second >= 64 && second <= 127 ||
                    first == 192 && second == 0 || first == 198 && (second == 18 || second == 19);
        }
        // Only globally routable IPv6 addresses are considered for this local build.
        return bytes.length != 16 || (bytes[0] & 0xe0) != 0x20;
    }

    private static String extract(Document doc) {
        String description = doc.select("meta[name=description]").attr("content");
        List<String> headings = doc.select("h1,h2,h3").stream().limit(9)
                .map(Element::text).filter(text -> !text.isBlank()).map(text -> limit(text, 75)).toList();
        List<String> links = doc.select("a[href]").stream()
                .filter(a -> pageType(a.attr("href") + " " + a.text()) != null)
                .map(a -> limit(a.text().strip(), 45)).filter(text -> !text.isBlank())
                .distinct().limit(8).toList();
        List<String> booking = doc.select("a[href]").stream()
                .filter(a -> pageType(a.attr("href") + " " + a.text()) != null &&
                        "BOOKING".equals(pageType(a.attr("href") + " " + a.text())))
                .map(a -> limit(a.text(), 35) + " (" + limit(a.attr("abs:href"), 150) + ")")
                .distinct().limit(3).toList();
        return limit("Meta description: " + limit(description, 180)
                + "; visible headings: " + String.join(" | ", headings)
                + "; visible relevant link labels: " + String.join(" | ", links)
                + "; booking links observed: " + (booking.isEmpty() ? "none confirmed by this scan" : String.join(" | ", booking))
                + ". Presence of a link is not verification that it works; missing links are not proof of absence.", 1200);
    }

    private static String extractDetail(Document doc) {
        List<String> headings = doc.select("h1,h2,h3").stream().limit(8)
                .map(Element::text).filter(t -> !t.isBlank()).map(t -> limit(t, 90)).toList();
        Element main = doc.selectFirst("main,article,[role=main]");
        String visible = main == null ? doc.body().text() : main.text();
        visible = visible.replaceAll("\\s+", " ").strip();
        java.util.regex.Matcher prices = java.util.regex.Pattern
                .compile("(?:\\$|USD\\s)\\s?\\d{1,4}(?:\\.\\d{2})?")
                .matcher(visible);
        List<String> examples = new java.util.ArrayList<>();
        while (prices.find() && examples.size() < 4) examples.add(prices.group());
        return limit("Page heading samples: " + String.join(" | ", headings)
                + "; price-like text samples (not verified offers): " + String.join(" | ", examples)
                + "; visible page excerpt: " + limit(visible, 1120)
                + ". Page text is evidence of what was published, not proof of actual availability or price.", 2000);
    }

    private static String hostname(String address) {
        try { return URI.create(address.strip()).getHost().toLowerCase(Locale.ROOT)
                .replaceFirst("^www\\.", ""); }
        catch (Exception ignored) { return ""; }
    }
    private static String limit(String value, int length) {
        return value == null ? "" : value.strip().substring(0, Math.min(value.strip().length(), length));
    }
}
