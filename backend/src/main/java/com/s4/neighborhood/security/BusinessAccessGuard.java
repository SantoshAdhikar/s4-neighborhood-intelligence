package com.s4.neighborhood.security;

import com.s4.neighborhood.repository.BusinessProfileRepository;
import com.s4.neighborhood.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Component
public class BusinessAccessGuard {
    private final BusinessProfileRepository businesses;
    private final AccountService accounts;

    public BusinessAccessGuard(BusinessProfileRepository businesses, AccountService accounts) {
        this.businesses = businesses;
        this.accounts = accounts;
    }

    public void requireOwner(UUID businessId) {
        if (!businesses.existsByIdAndOwnerUserId(businessId, accounts.currentUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found");
        }
    }
}
