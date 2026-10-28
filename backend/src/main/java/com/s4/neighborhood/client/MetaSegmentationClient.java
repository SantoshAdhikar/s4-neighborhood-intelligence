package com.s4.neighborhood.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class MetaSegmentationClient {
    private static final int MAX_STREAM_CHARS = 12_000_000;
    private final HttpClient client;
    private final JsonMapper json;
    private final URI responsesUri;
    private final String apiKey;
    private final String model;

    public MetaSegmentationClient(
            @Value("${s4.meta.base-url}") String baseUrl,
            @Value("${s4.meta.api-key}") String apiKey,
            @Value("${s4.sam.model:sam-3.1}") String model,
            JsonMapper json
    ) {
        URI uri = URI.create(baseUrl);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("Meta Model API URL must be HTTPS");
        }
        this.responsesUri = URI.create(baseUrl.replaceAll("/+$", "") + "/responses");
        this.apiKey = apiKey;
        this.model = model;
        this.json = json;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    public Segmentation segment(Path image, String mimeType, String concept) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("MODEL_API_KEY is not configured on the backend");
        }
        if (!"image/png".equals(mimeType) && !"image/jpeg".equals(mimeType)) {
            throw new IllegalArgumentException("SAM 3.1 currently supports saved images in this workflow");
        }
        try {
            String dataUrl = "data:" + mimeType + ";base64,"
                    + Base64.getEncoder().encodeToString(Files.readAllBytes(image));
            Map<String, Object> request = Map.of(
                    "model", model,
                    "input", List.of(Map.of(
                            "type", "message", "role", "user",
                            "content", List.of(
                                    Map.of("type", "input_text", "text", concept),
                                    Map.of("type", "input_image", "image_url", dataUrl)))),
                    "stream", true,
                    "metadata", Map.of("mask_encoding", "one_bit", "include_confidence", "true"));
            HttpRequest httpRequest = HttpRequest.newBuilder(responsesUri)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .timeout(Duration.ofSeconds(240))
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(request)))
                    .build();
            HttpResponse<java.io.InputStream> response = client.send(
                    httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                try (var ignored = response.body()) {
                    // Do not expose provider response bodies: they can contain sensitive details.
                }
                throw new IllegalStateException("Meta SAM request failed (HTTP " + response.statusCode() + ")");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    response.body(), StandardCharsets.UTF_8))) {
                return readEvents(reader);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("SAM request was interrupted", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("SAM request failed due to a connection or file error", exception);
        }
    }

    Segmentation readEvents(BufferedReader reader) throws IOException {
        StringBuilder deltas = new StringBuilder();
        String finalText = null;
        String responseId = null;
        boolean completed = false;
        boolean finalized = false;
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("data:")) continue;
            String data = line.substring(5).trim();
            if (data.equals("[DONE]")) break;
            if (data.length() > MAX_STREAM_CHARS) throw new IllegalStateException("SAM event exceeded size limit");
            Map<?, ?> event = json.readValue(data, Map.class);
            Object kind = event.get("type");
            if (!(kind instanceof String type)) continue;
            switch (type) {
                case "response.output_text.delta" -> {
                    if (finalized) throw new IllegalStateException("Meta returned output after finalization");
                    Object delta = event.get("delta");
                    if (delta instanceof String text) deltas.append(text);
                }
                case "response.output_text.done" -> {
                    Object text = event.get("text");
                    if (!(text instanceof String value)) {
                        throw new IllegalStateException("SAM returned an invalid text finalizer");
                    }
                    finalText = value;
                    finalized = true;
                }
                case "response.content_part.done" -> {
                    if (event.get("part") instanceof Map<?, ?> part
                            && "output_text".equals(part.get("type"))
                            && part.get("text") instanceof String value) {
                        if (finalText != null && !finalText.equals(value)) {
                            throw new IllegalStateException("SAM final text mismatch");
                        }
                        finalText = value;
                        finalized = true;
                    }
                }
                case "response.completed" -> {
                    completed = true;
                    if (event.get("response") instanceof Map<?, ?> response
                            && response.get("id") instanceof String id) responseId = id;
                }
                case "response.failed", "response.incomplete", "error" ->
                        throw new IllegalStateException("SAM returned an incomplete or failed response");
                default -> { }
            }
            if (deltas.length() > MAX_STREAM_CHARS
                    || (finalText != null && finalText.length() > MAX_STREAM_CHARS)) {
                throw new IllegalStateException("SAM output exceeded size limit");
            }
        }
        if (!completed || !finalized || responseId == null || responseId.isBlank()) {
            throw new IllegalStateException("SAM stream ended without a completed response");
        }
        if (!deltas.isEmpty() && !deltas.toString().equals(finalText)) {
            throw new IllegalStateException("SAM streamed output did not match final response");
        }
        return new Segmentation(model, responseId, finalText);
    }

    public record Segmentation(String model, String responseId, String rawOutput) {}
}
