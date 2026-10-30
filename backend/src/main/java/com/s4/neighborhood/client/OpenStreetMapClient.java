
package com.s4.neighborhood.client;

import com.s4.neighborhood.entity.BusinessProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class OpenStreetMapClient {
    private static final Object GEOCODE_LOCK = new Object();
    private static long nextGeocodeNanos;

    private final RestClient http;
    private final String userAgent;

    public OpenStreetMapClient(@Value("${S4_OSM_USER_AGENT:}") String userAgent) {
        this.userAgent = userAgent;

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(8))
                        .build());

        factory.setReadTimeout(Duration.ofSeconds(50));

        this.http = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    public Location geocode(BusinessProfile business) {
        ensureIdentified();

        String address = String.join(", ",
                business.getStreetAddress(),
                business.getCity(),
                business.getState(),
                business.getPostalCode());

        URI uri = UriComponentsBuilder
                .fromUriString("https://nominatim.openstreetmap.org/search")
                .queryParam("q", address)
                .queryParam("format", "jsonv2")
                .queryParam("addressdetails", 1)
                .queryParam("limit", 1)
                .build()
                .encode()
                .toUri();

        try {
            Object response;

            synchronized (GEOCODE_LOCK) {
                long remaining = nextGeocodeNanos - System.nanoTime();

                if (remaining > 0) {
                    TimeUnit.NANOSECONDS.sleep(remaining);
                }

                nextGeocodeNanos = System.nanoTime()
                        + TimeUnit.SECONDS.toNanos(1);

                response = http.get()
                        .uri(uri)
                        .header(HttpHeaders.USER_AGENT, userAgent)
                        .retrieve()
                        .body(List.class);
            }

            if (!(response instanceof List<?> results)
                    || results.isEmpty()
                    || !(results.getFirst() instanceof Map<?, ?> match)) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Address could not be located. Verify the street, city, state and ZIP code.");
            }

            Object parts = match.get("address");

            if (parts instanceof Map<?, ?> details) {
                String postalCode = value(details.get("postcode"));

                if (!postalCode.isBlank()
                        && !postalCode.equalsIgnoreCase(business.getPostalCode())) {
                    throw new ResponseStatusException(
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "Map location ZIP does not match the saved address. Verify the business profile.");
                }
            }

            double lat = Double.parseDouble(value(match.get("lat")));
            double lon = Double.parseDouble(value(match.get("lon")));

            if (!Double.isFinite(lat) || !Double.isFinite(lon)
                    || lat < -90 || lat > 90
                    || lon < -180 || lon > 180) {
                throw new IllegalArgumentException("Invalid geocoding coordinates");
            }

            return new Location(
                    lat,
                    lon,
                    value(match.get("display_name")));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Geocoding interrupted",
                    exception);
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Map address lookup failed. Try again later.",
                    exception);
        }
    }

    public List<OsmPlace> findNearby(
            Location center,
            int radiusMiles,
            String industry
    ) {
        ensureIdentified();

        int meters = (int) Math.round(radiusMiles * 1609.344);

        String point = String.format(
                Locale.ROOT,
                "(around:%d,%.6f,%.6f)",
                meters,
                center.latitude(),
                center.longitude());

        String filters = switch (industry.toLowerCase(Locale.ROOT)) {
            case "beauty & personal care" ->
                    "nwr" + point
                            + "[\"shop\"~\"^(beauty|hairdresser|cosmetics|massage|tattoo)$\"];"
                            + "nwr" + point
                            + "[\"amenity\"=\"spa\"];";

            case "restaurant & food" ->
                    "nwr" + point
                            + "[\"amenity\"~\"^(restaurant|cafe|fast_food|bar)$\"];"
                            + "nwr" + point
                            + "[\"shop\"=\"bakery\"];";

            case "retail" ->
                    "nwr" + point
                            + "[\"shop\"~\"^(supermarket|convenience|wholesale|general|department_store|variety_store|greengrocer|grocery|spices|beverages|deli|butcher|bakery|clothes|shoes|hardware|electronics|books|beauty|cosmetics|gift|houseware|mall)$\"];";

            case "health & wellness" ->
                    "nwr" + point
                            + "[\"amenity\"~\"^(clinic|doctors|dentist|pharmacy)$\"];"
                            + "nwr" + point
                            + "[\"leisure\"=\"fitness_centre\"];";

            case "automotive" ->
                    "nwr" + point
                            + "[\"shop\"~\"^(car|car_repair|car_parts|tyres)$\"];";

            case "home services" ->
                    "nwr" + point
                            + "[\"craft\"~\"^(plumber|electrician|carpenter|painter|hvac)$\"];";

            case "professional services" ->
                    "nwr" + point
                            + "[\"office\"~\"^(lawyer|accountant|insurance|estate_agent)$\"];";

            case "pet services" ->
                    "nwr" + point
                            + "[\"shop\"~\"^(pet|pet_grooming)$\"];"
                            + "nwr" + point + "[\"amenity\"=\"veterinary\"];";

            case "education & tutoring" ->
                    "nwr" + point
                            + "[\"amenity\"~\"^(school|driving_school|language_school|music_school)$\"];"
                            + "nwr" + point + "[\"office\"=\"educational_institution\"];";

            case "technology & repair" ->
                    "nwr" + point
                            + "[\"shop\"~\"^(computer|mobile_phone|electronics|electrical|repair)$\"];"
                            + "nwr" + point + "[\"craft\"=\"electronics_repair\"];";

            case "hospitality & lodging" ->
                    "nwr" + point + "[\"tourism\"~\"^(hotel|motel|hostel|guest_house)$\"];";

            case "cleaning & laundry" ->
                    "nwr" + point + "[\"shop\"~\"^(laundry|dry_cleaning)$\"];";

            case "events & photography" ->
                    "nwr" + point + "[\"craft\"=\"photographer\"];"
                            + "nwr" + point + "[\"office\"=\"event_planner\"];";

            case "childcare" ->
                    "nwr" + point + "[\"amenity\"~\"^(kindergarten|childcare)$\"];";

            default ->
                    "nwr" + point + "[\"shop\"~\"^(convenience|clothes|beauty|hairdresser|bakery|florist|hardware|supermarket|laundry|pet)$\"];"
                            + "nwr" + point + "[\"craft\"~\"^(photographer|plumber|electrician|carpenter)$\"];"
                            + "nwr" + point + "[\"office\"~\"^(lawyer|accountant|estate_agent)$\"];";
        };

        String query = "[out:json][timeout:25];("
                + filters
                + ");out center 500;";

        String form = "data=" + URLEncoder.encode(
                query, StandardCharsets.UTF_8);

        try {
            Map<?, ?> response = http.post()
                    .uri("https://overpass-api.de/api/interpreter")
                    .header(HttpHeaders.USER_AGENT, userAgent)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (response == null
                    || !(response.get("elements") instanceof List<?> elements)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Neighborhood provider returned an invalid response.");
            }

            if (response.containsKey("remark")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Neighborhood provider could not finish this search. Try again later.");
            }

            List<OsmPlace> places = new ArrayList<>();

            for (Object entry : elements) {
                if (!(entry instanceof Map<?, ?> item)
                        || !(item.get("tags") instanceof Map<?, ?> tags)) {
                    continue;
                }

                String name = value(tags.get("name"));
                String type = value(item.get("type"));

                if (name.isBlank()
                        || !Set.of("node", "way", "relation").contains(type)) {
                    continue;
                }

                Object pointData = item.get("center") instanceof Map<?, ?> map
                        ? map : item;

                if (!(pointData instanceof Map<?, ?> coordinates)) {
                    continue;
                }

                double lat = number(coordinates.get("lat"));
                double lon = number(coordinates.get("lon"));

                if (!Double.isFinite(lat) || !Double.isFinite(lon)
                        || Math.abs(lat) > 90 || Math.abs(lon) > 180) {
                    continue;
                }

                String category = category(tags);
                String website = value(tags.get("website"));

                if (website.isBlank()) {
                    website = value(tags.get("contact:website"));
                }

                if (!website.startsWith("https://")
                        && !website.startsWith("http://")) {
                    website = "";
                }

                long id = ((Number) item.get("id")).longValue();

                places.add(new OsmPlace(
                        type,
                        id,
                        name,
                        category,
                        lat,
                        lon,
                        website));
            }

            return places;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Neighborhood provider is unavailable. Try again later.",
                    exception);
        }
    }

    private void ensureIdentified() {
        if (userAgent.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Set S4_OSM_USER_AGENT with your application name and contact before using OpenStreetMap.");
        }
    }

    private static String category(Map<?, ?> tags) {
        for (String key : List.of("shop", "amenity", "office", "craft", "leisure", "tourism")) {
            String type = value(tags.get(key));
            if (type.isBlank()) continue;
            StringBuilder result = new StringBuilder(key + ": " + type.replace('_', ' '));
            // Retain contributed service signals where present. These are listing claims,
            // not independently verified offerings.
            for (String detail : List.of("beauty", "cuisine", "service")) {
                String v = value(tags.get(detail));
                if (!v.isBlank()) result.append("; ").append(detail).append(": ").append(v.replace('_', ' '));
            }
            return result.substring(0, Math.min(result.length(), 150));
        }
        return "Local business";
    }

    private static String value(Object object) {
        return object == null ? "" : object.toString().trim();
    }

    private static double number(Object value) {
        return value instanceof Number number
                ? number.doubleValue() : Double.NaN;
    }

    public record Location(
            double latitude,
            double longitude,
            String label
    ) {}

    public record OsmPlace(
            String type,
            long id,
            String name,
            String category,
            double latitude,
            double longitude,
            String website
    ) {}
}
