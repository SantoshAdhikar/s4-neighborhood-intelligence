package com.s4.neighborhood.service;

import com.s4.neighborhood.entity.AppUser;
import com.s4.neighborhood.entity.EmailVerificationToken;
import com.s4.neighborhood.repository.AppUserRepository;
import com.s4.neighborhood.repository.EmailVerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {
    @Mock AppUserRepository users;
    @Mock EmailVerificationTokenRepository tokens;
    @Mock ObjectProvider<JavaMailSender> senders;
    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-30T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void emailIsNotSilentlyMarkedVerifiedWhenSmtpIsNotConfigured() {
        EmailVerificationService service = new EmailVerificationService(users, tokens,
                senders, false, "", "http://localhost:5173", NOW);
        assertFalse(service.sendIfConfigured("owner@example.com"));
        verifyNoInteractions(users, tokens, senders);
    }

    @Test
    void expiredTokenFailsWithoutVerifyingAccount() {
        EmailVerificationService service = new EmailVerificationService(users, tokens,
                senders, false, "", "http://localhost:5173", NOW);
        String raw = "a".repeat(64);
        EmailVerificationToken token = new EmailVerificationToken();
        token.setExpiresAt(Instant.parse("2026-10-29T12:00:00Z"));
        when(tokens.findByTokenSha256(EmailVerificationService.sha256(raw))).thenReturn(Optional.of(token));
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.confirm(raw));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(users);
    }

    @Test
    void validTokenIsConsumedAndMarksOnlyReferencedAccountVerified() {
        EmailVerificationService service = new EmailVerificationService(users, tokens,
                senders, false, "", "http://localhost:5173", NOW);
        String raw = "b".repeat(64);
        UUID userId = UUID.randomUUID();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId(userId);
        token.setExpiresAt(Instant.parse("2026-10-31T12:00:00Z"));
        AppUser user = new AppUser();
        when(tokens.findByTokenSha256(EmailVerificationService.sha256(raw))).thenReturn(Optional.of(token));
        when(users.findById(userId)).thenReturn(Optional.of(user));
        service.confirm(raw);
        assertTrue(user.isEmailVerified());
        assertEquals(NOW.instant(), token.getConsumedAt());
        verify(users).save(user);
    }
}
