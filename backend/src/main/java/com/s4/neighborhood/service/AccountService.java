package com.s4.neighborhood.service;

import com.s4.neighborhood.dto.AccountRegistrationRequest;
import com.s4.neighborhood.dto.AccountResponse;
import com.s4.neighborhood.entity.AppUser;
import com.s4.neighborhood.repository.AppUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

@Service
public class AccountService {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public AccountService(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional
    public AccountResponse register(AccountRegistrationRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setDisplayName(request.displayName().trim());
        user.setPasswordHash(encoder.encode(request.password()));
        try {
            return toResponse(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account with this email already exists", exception);
        }
    }

    @Transactional(readOnly = true)
    public AccountResponse currentAccount() {
        return toResponse(currentUser());
    }

    @Transactional(readOnly = true)
    public UUID currentUserId() {
        return currentUser().getId();
    }

    private AppUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in required");
        }
        return users.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session account not found"));
    }

    private static AccountResponse toResponse(AppUser user) {
        return new AccountResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.isEmailVerified());
    }
}
