package com.s4.neighborhood.controller;

import com.s4.neighborhood.dto.StorefrontMediaResponse;
import com.s4.neighborhood.service.StorefrontMediaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/storefront-media")
public class StorefrontMediaController {
    private final StorefrontMediaService service;

    public StorefrontMediaController(StorefrontMediaService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public StorefrontMediaResponse upload(@PathVariable UUID businessId,
                                          @RequestPart("file") MultipartFile file, HttpServletRequest request) {
        localOnly(request);
        return service.upload(businessId, file);
    }

    @GetMapping
    public List<StorefrontMediaResponse> list(@PathVariable UUID businessId,
                                              HttpServletRequest request) {
        localOnly(request);
        return service.list(businessId);
    }

    @GetMapping("/{mediaId}/file")
    public ResponseEntity<FileSystemResource> media(@PathVariable UUID businessId,
                                                    @PathVariable UUID mediaId, HttpServletRequest request) {
        localOnly(request);
        StorefrontMediaService.MediaFile media = service.file(businessId, mediaId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.mimeType()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(media.path()));
    }

    @PostMapping("/{mediaId}/analyze")
    public StorefrontMediaResponse analyze(@PathVariable UUID businessId,
                                           @PathVariable UUID mediaId, HttpServletRequest request) {
        localOnly(request);
        return service.analyze(businessId, mediaId);
    }

    private static void localOnly(HttpServletRequest request) {
        try {
            if (InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress()) {
                return;
            }
        } catch (UnknownHostException ignored) {
            // Unrecognized addresses must not bypass the local-only restriction.
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Storefront media endpoints require localhost until user authentication is implemented");
    }
}
