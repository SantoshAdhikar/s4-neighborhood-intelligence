
package com.s4.neighborhood.controller;

import com.s4.neighborhood.dto.NeighborhoodResponse;
import com.s4.neighborhood.service.NeighborhoodResearchService;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/neighborhood")
public class NeighborhoodController {
    private final NeighborhoodResearchService service;

    public NeighborhoodController(NeighborhoodResearchService service) {
        this.service = service;
    }

    @PostMapping
    public NeighborhoodResponse research(@PathVariable UUID businessId) {
        return service.research(businessId);
    }
}
