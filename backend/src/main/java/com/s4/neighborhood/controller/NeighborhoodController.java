
package com.s4.neighborhood.controller;

import com.s4.neighborhood.dto.NeighborhoodResponse;
import com.s4.neighborhood.service.NeighborhoodResearchService;
import com.s4.neighborhood.security.BusinessAccessGuard;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/neighborhood")
public class NeighborhoodController {
    private final NeighborhoodResearchService service;
    private final BusinessAccessGuard access;

    public NeighborhoodController(NeighborhoodResearchService service, BusinessAccessGuard access) {
        this.service = service;
        this.access = access;
    }

    @PostMapping
    public NeighborhoodResponse research(@PathVariable UUID businessId) {
        access.requireOwner(businessId);
        return service.research(businessId);
    }
}
