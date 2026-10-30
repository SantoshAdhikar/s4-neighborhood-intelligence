package com.s4.neighborhood.controller;

import com.s4.neighborhood.dto.AccountRegistrationRequest;
import com.s4.neighborhood.dto.AccountResponse;
import com.s4.neighborhood.dto.EmailVerificationRequest;
import com.s4.neighborhood.dto.PasswordResetRequest;
import com.s4.neighborhood.dto.PasswordResetConfirmRequest;
import com.s4.neighborhood.service.PasswordResetService;
import com.s4.neighborhood.service.AccountService;
import com.s4.neighborhood.service.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AccountController {
    private final AccountService accounts;
    private final EmailVerificationService emails;
    private final PasswordResetService resets;

    public AccountController(AccountService accounts, EmailVerificationService emails, PasswordResetService resets) {
        this.accounts = accounts;
        this.emails = emails;
        this.resets = resets;
    }

    @GetMapping("/csrf")
    public void csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse register(@Valid @RequestBody AccountRegistrationRequest request) {
        AccountResponse account = accounts.register(request);
        // Email setup is optional in this local hackathon build. The account remains unverified.
        try {
            emails.sendIfConfigured(account.email());
        } catch (org.springframework.web.server.ResponseStatusException deliveryFailure) {
            // Registration succeeded; the owner can sign in and resend after SMTP is fixed.
        }
        return account;
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(@Valid @RequestBody EmailVerificationRequest request) {
        emails.confirm(request.token());
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resend() {
        if (!emails.sendIfConfigured(accounts.currentAccount().email())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Email verification is not configured");
        }
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody PasswordResetRequest request) {
        resets.request(request.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        resets.confirm(request.token(), request.password());
    }

    @GetMapping("/me")
    public AccountResponse me() {
        return accounts.currentAccount();
    }
}
