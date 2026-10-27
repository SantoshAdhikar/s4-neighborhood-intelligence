
package com.s4.neighborhood.controller;

import com.s4.neighborhood.dto.BusinessProfileRequest;
import com.s4.neighborhood.dto.BusinessProfileResponse;
import com.s4.neighborhood.service.BusinessProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/businesses")
public class BusinessProfileController {

    private final BusinessProfileService service;

    public BusinessProfileController(BusinessProfileService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessProfileResponse create(
            @Valid @RequestBody BusinessProfileRequest request
    ) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public BusinessProfileResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public BusinessProfileResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody BusinessProfileRequest request
    ) {
        return service.update(id, request);
    }
}
