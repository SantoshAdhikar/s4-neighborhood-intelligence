package com.s4.neighborhood.controller;

import com.s4.neighborhood.dto.SegmentationRequest;
import com.s4.neighborhood.dto.SegmentationResponse;
import com.s4.neighborhood.security.BusinessAccessGuard;
import com.s4.neighborhood.service.StorefrontSegmentationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/storefront-media/{mediaId}/segmentations")
public class StorefrontSegmentationController {
    private final StorefrontSegmentationService service;
    private final BusinessAccessGuard access;

    public StorefrontSegmentationController(StorefrontSegmentationService service, BusinessAccessGuard access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping
    public List<SegmentationResponse> list(@PathVariable UUID businessId, @PathVariable UUID mediaId) {
        access.requireOwner(businessId);
        return service.list(businessId, mediaId);
    }

    @PostMapping
    public SegmentationResponse segment(@PathVariable UUID businessId, @PathVariable UUID mediaId,
                                        @Valid @RequestBody SegmentationRequest request) {
        access.requireOwner(businessId);
        return service.segment(businessId, mediaId, request.concept());
    }
}
