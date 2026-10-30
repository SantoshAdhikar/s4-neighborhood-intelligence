package com.s4.neighborhood.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Component
public class OverturePlaceCatalog {
    private static final Logger log = LoggerFactory.getLogger(OverturePlaceCatalog.class);
    private static final long MAX_FILE_BYTES = 100L * 1024 * 1024;
    private static final int MAX_LOCAL_RECORDS = 15000;
    private static final Duration CACHE_LIFETIME = Duration.ofDays(7);

    private final JsonMapper json;
    private final Path directory;
    private final String command;
    private final boolean enabled;

    public OverturePlaceCatalog(JsonMapper json,
                                @Value("${S4_OVERTURE_CACHE_DIR:data/overture}") String cacheDirectory,
                                @Value("${S4_OVERTURE_COMMAND:overturemaps}") String command,
                                @Value("${S4_OVERTURE_ENABLED:true}") boolean enabled) {
        this.json = json;
        this.directory = Path.of(cacheDirectory).toAbsolutePath().normalize();
        this.command = command;
        this.enabled = enabled;
    }

    public List<Place> nearby(double latitude, double longitude, int radiusMiles) {
        if (!enabled || radiusMiles < 1 || radiusMiles > 20 ||
                !Double.isFinite(latitude) || !Double.isFinite(longitude)) return List.of();
        try {
            Files.createDirectories(directory);
            String fileName = String.format(Locale.ROOT, "places_%+.3f_%+.3f_%d.geojson",
                    latitude, longitude, radiusMiles);
            Path file = directory.resolve(fileName);
            if (!Files.isRegularFile(file) || Files.size(file) == 0 ||
                    Files.getLastModifiedTime(file).toInstant().isBefore(Instant.now().minus(CACHE_LIFETIME))) {
                download(file, latitude, longitude, radiusMiles);
            }
            if (!Files.isRegularFile(file) || Files.size(file) == 0 || Files.size(file) > MAX_FILE_BYTES) {
                return List.of();
            }
            List<Place> available = parse(Files.readString(file), latitude, longitude, radiusMiles);
            log.info("Overture places cache: {} places within {} miles (file {})",
                    available.size(), radiusMiles, file.getFileName());
            return available;
        } catch (Exception problem) {
            log.warn("Overture enrichment unavailable; continuing with OSM: {}", problem.getMessage());
            return List.of();
        }
    }

    private synchronized void download(Path file, double latitude, double longitude, int radiusMiles) {
        Path temporary = file.resolveSibling(file.getFileName() + ".partial.geojson");
        Path diagnosticLog = file.resolveSibling(file.getFileName() + ".download.log");
        try {
            if (Files.isRegularFile(file) && Files.size(file) > 0 &&
                    Files.getLastModifiedTime(file).toInstant().isAfter(Instant.now().minus(CACHE_LIFETIME))) return;
            double padMiles = radiusMiles * 1.2;
            double deltaLatitude = padMiles / 69.0;
            double deltaLongitude = padMiles / (69.0 * Math.max(0.2, Math.cos(Math.toRadians(latitude))));
            String bbox = String.format(Locale.ROOT, "%.6f,%.6f,%.6f,%.6f",
                    longitude - deltaLongitude, latitude - deltaLatitude,
                    longitude + deltaLongitude, latitude + deltaLatitude);
            Files.deleteIfExists(temporary);
            Files.deleteIfExists(diagnosticLog);
            ProcessBuilder builder = new ProcessBuilder(command, "download", "--bbox=" + bbox,
                    "-f", "geojson", "--type=place", "-o", temporary.toString());
            builder.redirectErrorStream(true);
            builder.redirectOutput(diagnosticLog.toFile());
            log.info("Downloading Overture places for radius {} miles with configured CLI", radiusMiles);
            Process process = builder.start();
            if (!process.waitFor(150, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                throw new IOException("Overture download timed out after 150 seconds. " + lastOutput(diagnosticLog));
            }
            if (process.exitValue() != 0 || !Files.isRegularFile(temporary) ||
                    Files.size(temporary) == 0 || Files.size(temporary) > MAX_FILE_BYTES) {
                throw new IOException("Overture download exited with code " + process.exitValue()
                        + ". " + lastOutput(diagnosticLog));
            }
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            log.info("Cached Overture places extract at {}", file);
        } catch (Exception failure) {
            log.warn("Overture download unavailable: {}", failure.getMessage());
        } finally {
            try {
                Files.deleteIfExists(temporary);
                Files.deleteIfExists(diagnosticLog);
            } catch (IOException ignored) {
                // A failed cleanup must not prevent saved OSM research.
            }
        }
    }

    private static String lastOutput(Path file) {
        if (!Files.isRegularFile(file)) return "No CLI output captured.";
        try (RandomAccessFile output = new RandomAccessFile(file.toFile(), "r")) {
            long count = Math.min(2048, output.length());
            byte[] tail = new byte[(int) count];
            output.seek(output.length() - count);
            output.readFully(tail);
            return new String(tail, StandardCharsets.UTF_8).replaceAll("\\s+", " ").strip();
        } catch (IOException exception) {
            return "Could not read CLI diagnostics.";
        }
    }

    List<Place> parse(String geoJson, double latitude, double longitude, int radiusMiles) {
        JsonNode root = json.readTree(geoJson);
        JsonNode features = root.path("features");
        if (!features.isArray()) return List.of();
        List<Place> found = new ArrayList<>();
        for (JsonNode feature : features) {
            JsonNode coordinates = feature.path("geometry").path("coordinates");
            if (!coordinates.isArray() || coordinates.size() < 2) continue;
            double longitude2 = coordinates.get(0).asDouble(Double.NaN);
            double latitude2 = coordinates.get(1).asDouble(Double.NaN);
            if (!Double.isFinite(latitude2) || !Double.isFinite(longitude2)) continue;
            double distance = distanceMiles(latitude, longitude, latitude2, longitude2);
            if (distance > radiusMiles) continue;
            JsonNode props = feature.path("properties");
            String name = text(props.path("names").path("primary"));
            if (name.isBlank()) name = text(props.path("name"));
            String id = text(props.path("id"));
            if (id.isBlank()) id = text(feature.path("id"));
            if (name.isBlank() || id.isBlank()) continue;
            JsonNode confidence = props.path("confidence");
            if (confidence.isNumber() && confidence.asDouble() < 0.6) continue;
            String category = text(props.path("taxonomy").path("primary"));
            if (category.isBlank()) category = text(props.path("basic_category"));
            String website = website(props.path("websites"));
            found.add(new Place(id, name, category, latitude2, longitude2, distance, website));
        }
        found.sort(Comparator.comparingDouble(Place::distanceMiles));
        return found.stream().limit(MAX_LOCAL_RECORDS).toList();
    }

    static String website(JsonNode websites) {
        if (websites == null) return "";
        if (websites.isArray()) {
            for (JsonNode site : websites) {
                String address = normalizeWebsite(site.isTextual() ? site.asText() : text(site.path("url")));
                if (!address.isBlank()) return address;
            }
        } else if (websites.isTextual()) {
            return normalizeWebsite(websites.asText());
        }
        return "";
    }

    private static String normalizeWebsite(String input) {
        if (input == null) return "";
        String url = input.strip();
        if (url.startsWith("http://")) url = "https://" + url.substring(7);
        else if (url.startsWith("www.")) url = "https://" + url;
        return url.startsWith("https://") && url.length() <= 500 ? url : "";
    }

    static double distanceMiles(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return 3958.76 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0, 1 - a)));
    }

    private static String text(JsonNode value) {
        return value != null && value.isTextual() ? value.asText().strip() : "";
    }

    public record Place(String id, String name, String category, double latitude,
                        double longitude, double distanceMiles, String website) {}
}
