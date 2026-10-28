package com.s4.neighborhood.controller;

import com.s4.neighborhood.dto.StorefrontMediaResponse;
import com.s4.neighborhood.service.StorefrontMediaService;
import com.s4.neighborhood.security.BusinessAccessGuard;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/storefront-media")
public class StorefrontMediaController {
    private final StorefrontMediaService service;
    private final BusinessAccessGuard access;

    public StorefrontMediaController(StorefrontMediaService service, BusinessAccessGuard access) {
        this.service = service;
        this.access = access;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public StorefrontMediaResponse upload(@PathVariable UUID businessId,
                                          @RequestPart("file") MultipartFile file) {
        access.requireOwner(businessId);
        return service.upload(businessId, file);
    }

    @GetMapping
    public List<StorefrontMediaResponse> list(@PathVariable UUID businessId) {
        access.requireOwner(businessId);
        return service.list(businessId);
    }

    @GetMapping("/{mediaId}/file")
    public ResponseEntity<FileSystemResource> media(@PathVariable UUID businessId,
                                                    @PathVariable UUID mediaId) {
        access.requireOwner(businessId);
        StorefrontMediaService.MediaFile media = service.file(businessId, mediaId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.mimeType()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(media.path()));
    }

    @PostMapping("/{mediaId}/analyze")
    public StorefrontMediaResponse analyze(@PathVariable UUID businessId,
                                           @PathVariable UUID mediaId) {
        access.requireOwner(businessId);
        return service.analyze(businessId, mediaId);
    }
}
