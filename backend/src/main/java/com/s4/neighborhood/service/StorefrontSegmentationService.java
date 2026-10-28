package com.s4.neighborhood.service;

import com.s4.neighborhood.client.MetaSegmentationClient;
import com.s4.neighborhood.dto.SegmentationResponse;
import com.s4.neighborhood.entity.StorefrontSegmentation;
import com.s4.neighborhood.repository.StorefrontSegmentationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class StorefrontSegmentationService {
    private final StorefrontMediaService media;
    private final StorefrontSegmentationRepository repository;
    private final MetaSegmentationClient meta;
    private final long maxImageBytes;
    private final int maxAttempts;

    public StorefrontSegmentationService(
            StorefrontMediaService media,
            StorefrontSegmentationRepository repository,
            MetaSegmentationClient meta,
            @Value("${s4.sam.max-image-bytes:8388608}") long maxImageBytes,
            @Value("${s4.sam.max-attempts:2}") int maxAttempts
    ) {
        this.media = media;
        this.repository = repository;
        this.meta = meta;
        this.maxImageBytes = maxImageBytes;
        this.maxAttempts = maxAttempts;
    }

    public List<SegmentationResponse> list(UUID businessId, UUID mediaId) {
        media.file(businessId, mediaId);
        return repository.findByMediaIdOrderByCreatedAtDesc(mediaId)
                .stream().map(StorefrontSegmentationService::toResponse).toList();
    }

    public SegmentationResponse segment(UUID businessId, UUID mediaId, String requestedConcept) {
        String concept = normalize(requestedConcept);
        StorefrontMediaService.MediaFile input = media.file(businessId, mediaId);
        if (!input.mimeType().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "SAM image segmentation supports PNG and JPEG only; video tracking is not enabled yet");
        }
        try {
            if (Files.size(input.path()) > maxImageBytes) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                        "This image is too large for the current SAM workflow (8 MB maximum)");
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Saved image could not be read", exception);
        }

        StorefrontSegmentation record = repository.findByMediaIdAndConcept(mediaId, concept)
                .orElseGet(() -> create(mediaId, concept));
        if ("COMPLETED".equals(record.getStatus())) return toResponse(record);
        if (repository.claim(record.getId(), maxAttempts) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Segmentation is processing, completed, or the retry limit was reached");
        }

        record = repository.findById(record.getId()).orElseThrow();
        try {
            MetaSegmentationClient.Segmentation result = meta.segment(input.path(), input.mimeType(), concept);
            record.setModelName(result.model());
            record.setModelResponseId(result.responseId());
            record.setRawOutput(result.rawOutput());
            record.setCompletedAt(Instant.now());
            record.setStatus("COMPLETED");
            record.setLastError(null);
        } catch (Exception exception) {
            record.setStatus("FAILED");
            String detail = exception.getMessage() == null ? "SAM segmentation failed" : exception.getMessage();
            record.setLastError(detail.substring(0, Math.min(detail.length(), 400)));
        }
        return toResponse(repository.saveAndFlush(record));
    }

    private StorefrontSegmentation create(UUID mediaId, String concept) {
        StorefrontSegmentation record = new StorefrontSegmentation();
        record.setMediaId(mediaId);
        record.setConcept(concept);
        return repository.saveAndFlush(record);
    }

    static String normalize(String input) {
        if (input == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter one visible object");
        String concept = input.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (concept.length() < 2 || concept.length() > 80
                || !concept.matches("[\\p{L}\\p{N}][\\p{L}\\p{N} .'-]*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use a short noun phrase (2–80 characters), such as storefront sign or glass door");
        }
        return concept;
    }

    private static SegmentationResponse toResponse(StorefrontSegmentation record) {
        return new SegmentationResponse(record.getId(), record.getMediaId(), record.getConcept(),
                record.getStatus(), record.getAttempts(), record.getModelName(),
                record.getModelResponseId(), record.getRawOutput(), record.getLastError(),
                record.getCreatedAt(), record.getCompletedAt());
    }
}
