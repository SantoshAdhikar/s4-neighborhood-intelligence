package com.s4.neighborhood.controller;

import com.s4.neighborhood.dto.AccountRegistrationRequest;
import com.s4.neighborhood.dto.AccountResponse;
import com.s4.neighborhood.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) { this.accounts = accounts; }

    @GetMapping("/csrf")
    public void csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse register(@Valid @RequestBody AccountRegistrationRequest request) {
        return accounts.register(request);
    }

    @GetMapping("/me")
    public AccountResponse me() {
        return accounts.currentAccount();
    }
}
