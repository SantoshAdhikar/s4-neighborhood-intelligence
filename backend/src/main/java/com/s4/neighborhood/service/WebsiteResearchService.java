package com.s4.neighborhood.service;

import com.s4.neighborhood.entity.AnalysisRun;
import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.entity.NearbyPlace;
import com.s4.neighborhood.entity.WebsiteFinding;
import com.s4.neighborhood.repository.NearbyPlaceRepository;
import com.s4.neighborhood.repository.NeighborhoodResearchRepository;
import com.s4.neighborhood.repository.WebsiteFindingRepository;
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
    private static final String USER_AGENT = "S4NeighborhoodIntelligence/0.1 (+https://github.com/SantoshAdhikar/s4-neighborhood-intelligence)";
    private final WebsiteFindingRepository findings;
    private final NeighborhoodResearchRepository researches;
    private final NearbyPlaceRepository places;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public WebsiteResearchService(WebsiteFindingRepository findings,
                                  NeighborhoodResearchRepository researches, NearbyPlaceRepository places) {
        this.findings = findings;
        this.researches = researches;
        this.places = places;
    }

    // Keep all candidate listings; limit external homepage requests per analysis.
    public void research(AnalysisRun run, BusinessProfile business) {
        if (business.getWebsite() != null && !business.getWebsite().isBlank()) {
            inspect(run, "OWNER_WEBSITE", business.getName(), business.getWebsite());
        }
        var latest = researches.findTopByBusinessIdAndAddressKeyAndRadiusMilesOrderByFetchedAtDesc(
                business.getId(), NeighborhoodResearchService.addressKey(business),
                business.getResearchRadiusMiles());
        if (latest.isEmpty()) return;
        java.util.Set<String> seenHosts = new java.util.HashSet<>();
        if (business.getWebsite() != null) seenHosts.add(hostname(business.getWebsite()));

        var matches = CompetitorSelectionService.select(business,
                places.findByResearchIdOrderByDistanceMilesAsc(latest.get().getId()));
        int inspectedHomepages = 0;
        for (var match : matches) {
            NearbyPlace place = match.place();
            String site = place.getWebsite();
            WebsiteFinding finding = new WebsiteFinding();
            finding.setRunId(run.getId());
            finding.setBusinessId(run.getBusinessId());
            finding.setKind("NEARBY_COMPETITOR");
            finding.setBusinessName(limit(place.getName(), 160));
            finding.setOsmSourceUrl(place.getSourceUrl());
            finding.setCategory(limit(place.getCategory(), 150));
            finding.setDistanceMiles(place.getDistanceMiles());
            finding.setMatchLevel(match.matchLevel());
            finding.setUrl(place.getSourceUrl());

            if (site == null || site.isBlank()) {
                finding.setStatus("NO_WEBSITE");
                finding.setNote("OSM has no website for this candidate. This does not mean the business lacks a website.");
            } else {
                String host = hostname(site);
                if (host.isBlank() || !seenHosts.add(host)) {
                    finding.setStatus("SKIPPED");
                    finding.setNote("Homepage not fetched: invalid or already inspected website host.");
                } else if (inspectedHomepages >= MAX_COMPETITOR_HOMEPAGES) {
                    finding.setStatus("SKIPPED");
                    finding.setNote("Candidate listed from OSM. Homepage inspection deferred to keep public-site requests bounded.");
                } else {
                    inspectedHomepages++;
                    finding.setUrl(limit(site, 500));
                    inspectHomepage(finding, site);
                }
            }
            findings.saveAndFlush(finding);
        }
    }

    private void inspect(AnalysisRun run, String kind, String name, String address) {
        WebsiteFinding finding = new WebsiteFinding();
        finding.setRunId(run.getId());
        finding.setBusinessId(run.getBusinessId());
        finding.setKind(kind);
        finding.setBusinessName(limit(name, 160));
        finding.setUrl(limit(address, 500));
        inspectHomepage(finding, address);
        findings.saveAndFlush(finding);
    }

    private void inspectHomepage(WebsiteFinding finding, String address) {
        try {
            URI uri = checkedUri(address);
            if (!allowedByRobots(uri)) {
                finding.setStatus("BLOCKED");
                finding.setNote("Website robots policy does not allow this automated inspection.");
            } else {
                HttpResponse<InputStream> response = request(uri);
                try (InputStream stream = response.body()) {
                    if (response.statusCode() != 200 ||
                            !response.headers().firstValue("Content-Type").orElse("")
                                    .toLowerCase(Locale.ROOT).contains("text/html")) {
                        finding.setStatus("UNAVAILABLE");
                        finding.setNote("Homepage did not return accessible HTML (HTTP " + response.statusCode() + ").");
                    } else {
                        byte[] bytes = stream.readNBytes(MAX_HTML_BYTES + 1);
                        boolean truncated = bytes.length > MAX_HTML_BYTES;
                        String html = new String(bytes, 0, Math.min(bytes.length, MAX_HTML_BYTES),
                                java.nio.charset.StandardCharsets.UTF_8);
                        Document document = Jsoup.parse(html, uri.toString());
                        finding.setStatus("FETCHED");
                        finding.setUrl(limit(uri.toString(), 500));
                        finding.setTitle(limit(document.title(), 250));
                        finding.setObservedSummary(extract(document));
                        finding.setNote(truncated
                                ? "Partial homepage: first 1 MiB of HTML inspected. Later content may be missing; no full audit."
                                : "Homepage HTML only. Missing labels do not prove a service is absent.");
                    }
                }
            }
        } catch (Exception exception) {
            finding.setStatus("UNAVAILABLE");
            finding.setNote("Website could not be inspected safely; no conclusion about business quality.");
        }
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
        List<String> headings = doc.select("h1,h2,h3").stream().limit(10)
                .map(Element::text).filter(text -> !text.isBlank()).map(text -> limit(text, 80)).toList();
        List<String> links = doc.select("a[href]").stream()
                .map(a -> a.text().strip()).filter(text -> text.matches("(?i).*(book|appoint|reserv|service|price|menu|contact|about).*"))
                .distinct().limit(8).map(text -> limit(text, 60)).toList();
        return limit("Meta description: " + limit(description, 220) +
                "; visible headings: " + String.join(" | ", headings) +
                "; relevant link labels: " + String.join(" | ", links) +
                ". These are extracted page labels, not a full usability, SEO, or service audit.", 1200);
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
