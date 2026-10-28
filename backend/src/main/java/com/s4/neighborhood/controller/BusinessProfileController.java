
package com.s4.neighborhood.controller;

import com.s4.neighborhood.dto.BusinessProfileRequest;
import com.s4.neighborhood.dto.BusinessProfileResponse;
import com.s4.neighborhood.service.BusinessProfileService;
import com.s4.neighborhood.service.AccountService;
import com.s4.neighborhood.security.BusinessAccessGuard;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/businesses")
public class BusinessProfileController {

    private final BusinessProfileService service;
    private final AccountService accounts;
    private final BusinessAccessGuard access;

    public BusinessProfileController(BusinessProfileService service, AccountService accounts, BusinessAccessGuard access) {
        this.service = service;
        this.accounts = accounts;
        this.access = access;
    }

    @GetMapping
    public List<BusinessProfileResponse> list() {
        return service.list(accounts.currentUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessProfileResponse create(
            @Valid @RequestBody BusinessProfileRequest request
    ) {
        return service.create(request, accounts.currentUserId());
    }

    @GetMapping("/{id}")
    public BusinessProfileResponse get(@PathVariable UUID id) {
        access.requireOwner(id);
        return service.get(id);
    }

    @PutMapping("/{id}")
    public BusinessProfileResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody BusinessProfileRequest request
    ) {
        access.requireOwner(id);
        return service.update(id, request);
    }
}
