package com.s4.neighborhood.controller;

import com.s4.neighborhood.dto.BusinessClaimResponse;
import com.s4.neighborhood.security.BusinessAccessGuard;
import com.s4.neighborhood.service.AccountService;
import com.s4.neighborhood.service.BusinessClaimService;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/claim")
public class BusinessClaimController {
    private final BusinessClaimService claims;
    private final BusinessAccessGuard access;
    private final AccountService accounts;

    public BusinessClaimController(BusinessClaimService claims, BusinessAccessGuard access,
                                   AccountService accounts) {
        this.claims = claims;
        this.access = access;
        this.accounts = accounts;
    }
    @GetMapping
    public BusinessClaimResponse status(@PathVariable UUID businessId) {
        access.requireOwner(businessId);
        return claims.status(businessId);
    }
    @PostMapping
    public BusinessClaimResponse request(@PathVariable UUID businessId) {
        access.requireOwner(businessId);
        return claims.requestReview(businessId, accounts.currentUserId());
    }
}
