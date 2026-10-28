package com.s4.neighborhood.service;

import com.s4.neighborhood.client.MetaVisionClient;
import com.s4.neighborhood.dto.StorefrontMediaResponse;
import com.s4.neighborhood.dto.VisualFindings;
import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.entity.StorefrontMedia;
import com.s4.neighborhood.repository.BusinessProfileRepository;
import com.s4.neighborhood.repository.StorefrontMediaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Service
public class StorefrontMediaService {
    private final BusinessProfileRepository businesses;
    private final StorefrontMediaRepository repository;
    private final MetaVisionClient meta;
    private final JsonMapper json;
    private final Path root;
    private final long maxBytes;
    private final int maxAttempts;

    public StorefrontMediaService(
            BusinessProfileRepository businesses,
            StorefrontMediaRepository repository,
            MetaVisionClient meta,
            JsonMapper json,
            @Value("${s4.media.root}") String root,
            @Value("${s4.media.max-bytes}") long maxBytes,
            @Value("${s4.media.max-analysis-attempts}") int maxAttempts
    ) {
        this.businesses = businesses;
        this.repository = repository;
        this.meta = meta;
        this.json = json;
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
        this.maxAttempts = maxAttempts;
    }

    public StorefrontMediaResponse upload(UUID businessId, MultipartFile file) {
        findBusiness(businessId);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an image or MP4 video");
        }
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds the configured upload limit");
        }

        MediaFormat format;
        try (InputStream stream = file.getInputStream()) {
            format = detectFormat(stream.readNBytes(16));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded media", exception);
        }

        if (format == null || !format.mimeType().equals(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only valid JPEG, PNG, and MP4 files are accepted");
        }

        String originalName = Optional.ofNullable(file.getOriginalFilename())
                .orElse("storefront")
                .replaceAll("[^a-zA-Z0-9._ -]", "_");
        if (originalName.length() > 200) originalName = originalName.substring(0, 200);
        String storageKey = UUID.randomUUID() + format.extension();
        Path destination = root.resolve(storageKey);

        try {
            Files.createDirectories(root);
            file.transferTo(destination);
            if (Files.size(destination) != file.getSize()) {
                throw new IOException("Uploaded file size mismatch");
            }

            StorefrontMedia media = new StorefrontMedia();
            media.setBusinessId(businessId);
            media.setOriginalFilename(originalName);
            media.setStorageKey(storageKey);
            media.setMimeType(format.mimeType());
            media.setSizeBytes(file.getSize());
            media.setSha256(hash(destination));
            return toResponse(repository.saveAndFlush(media));
        } catch (IOException | RuntimeException exception) {
            try { Files.deleteIfExists(destination); } catch (IOException ignored) { }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not store storefront media", exception);
        }
    }

    public List<StorefrontMediaResponse> list(UUID businessId) {
        findBusiness(businessId);
        return repository.findByBusinessIdOrderByCreatedAtDesc(businessId)
                .stream().map(this::toResponse).toList();
    }

    public MediaFile file(UUID businessId, UUID mediaId) {
        StorefrontMedia media = findMedia(businessId, mediaId);
        Path file = root.resolve(media.getStorageKey()).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media file was not found");
        }
        return new MediaFile(file, media.getMimeType());
    }

    public StorefrontMediaResponse analyze(UUID businessId, UUID mediaId) {
        BusinessProfile business = findBusiness(businessId);
        StorefrontMedia media = findMedia(businessId, mediaId);
        if ("ANALYZED".equals(media.getStatus())) return toResponse(media);

        if (repository.claimAnalysis(mediaId, businessId, maxAttempts) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Analysis is running, completed, or the attempt limit was reached");
        }

        media = findMedia(businessId, mediaId);
        try {
            MediaFile local = file(businessId, mediaId);
            MetaVisionClient.Analysis analysis = meta.analyze(local.path(), local.mimeType(), business);
            media.setAnalysisJson(analysis.rawJson());
            media.setModelName(analysis.model());
            media.setModelResponseId(analysis.responseId());
            media.setAnalyzedAt(Instant.now());
            media.setStatus("ANALYZED");
            media.setLastError(null);
        } catch (Exception exception) {
            media.setStatus("FAILED");
            String explanation = exception.getMessage();
            media.setLastError(explanation == null ? "Analysis failed" :
                    explanation.substring(0, Math.min(explanation.length(), 400)));
        }
        return toResponse(repository.saveAndFlush(media));
    }

    private BusinessProfile findBusiness(UUID id) {
        return businesses.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));
    }

    private StorefrontMedia findMedia(UUID businessId, UUID mediaId) {
        return repository.findByIdAndBusinessId(mediaId, businessId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Storefront media not found"));
    }

    private StorefrontMediaResponse toResponse(StorefrontMedia media) {
        VisualFindings findings = null;
        if (media.getAnalysisJson() != null) {
            findings = json.readValue(media.getAnalysisJson(), VisualFindings.class);
        }
        return new StorefrontMediaResponse(
                media.getId(), media.getBusinessId(), media.getOriginalFilename(),
                media.getMimeType(), media.getSizeBytes(), media.getSha256(),
                media.getStatus(), media.getAnalysisAttempts(), media.getModelName(),
                media.getModelResponseId(), findings, media.getLastError(),
                media.getCreatedAt(), media.getAnalyzedAt());
    }

    static MediaFormat detectFormat(byte[] header) {
        if (header.length >= 8 && header[0] == (byte) 0x89 && header[1] == 0x50
                && header[2] == 0x4e && header[3] == 0x47 && header[4] == 0x0d
                && header[5] == 0x0a && header[6] == 0x1a && header[7] == 0x0a) {
            return new MediaFormat("image/png", ".png");
        }
        if (header.length >= 3 && header[0] == (byte) 0xff
                && header[1] == (byte) 0xd8 && header[2] == (byte) 0xff) {
            return new MediaFormat("image/jpeg", ".jpg");
        }
        if (header.length >= 12 && header[4] == 'f' && header[5] == 't'
                && header[6] == 'y' && header[7] == 'p') {
            return new MediaFormat("video/mp4", ".mp4");
        }
        return null;
    }

    private static String hash(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int bytes;
                while ((bytes = input.read(buffer)) != -1) digest.update(buffer, 0, bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    record MediaFormat(String mimeType, String extension) {}
    public record MediaFile(Path path, String mimeType) {}
}
