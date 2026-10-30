package com.s4.neighborhood.service;

import com.s4.neighborhood.entity.AppUser;
import com.s4.neighborhood.entity.PasswordResetToken;
import com.s4.neighborhood.repository.AppUserRepository;
import com.s4.neighborhood.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
    @Mock AppUserRepository users;
    @Mock PasswordResetTokenRepository tokens;
    @Mock PasswordEncoder encoder;
    @Mock ObjectProvider<JavaMailSender> mail;

    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), ZoneOffset.UTC);

    private PasswordResetService service(boolean enabled) {
        return new PasswordResetService(users, tokens, encoder, mail, enabled, "s4@example.com", "http://localhost:5173", NOW);
    }

    @Test
    void resetRequiresConfiguredEmailTransport() {
        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                () -> service(false).request("user@example.com"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, failure.getStatusCode());
        verifyNoInteractions(users, tokens);
    }

    @Test
    void unknownEmailDoesNotExposeAccountExistenceOrCreateToken() {
        when(mail.getIfAvailable()).thenReturn(mock(JavaMailSender.class));
        when(users.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service(true).request("unknown@example.com"));
        verifyNoInteractions(tokens);
    }

    @Test
    void expiredLinkDoesNotChangePassword() {
        String raw = "a".repeat(64);
        PasswordResetToken token = new PasswordResetToken();
        token.setExpiresAt(NOW.instant().minusSeconds(1));
        when(tokens.findByTokenSha256(EmailVerificationService.sha256(raw))).thenReturn(Optional.of(token));
        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                () -> service(true).confirm(raw, "a-long-new-password"));
        assertEquals(HttpStatus.BAD_REQUEST, failure.getStatusCode());
        verifyNoInteractions(users, encoder);
    }

    @Test
    void resetConsumesEveryUnusedTokenForThatAccount() {
        String raw = "b".repeat(64);
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", userId);
        user.setEmail("owner@example.com");
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(userId);
        token.setExpiresAt(NOW.instant().plusSeconds(300));
        PasswordResetToken second = new PasswordResetToken();
        when(tokens.findByTokenSha256(EmailVerificationService.sha256(raw))).thenReturn(Optional.of(token));
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(encoder.encode("a-long-new-password")).thenReturn("bcrypt:encoded");
        when(tokens.findByUserIdAndConsumedAtIsNull(userId)).thenReturn(List.of(token, second));

        service(true).confirm(raw, "a-long-new-password");
        assertEquals("bcrypt:encoded", user.getPasswordHash());
        assertEquals(NOW.instant(), token.getConsumedAt());
        assertEquals(NOW.instant(), second.getConsumedAt());
        verify(users).save(user);
        verify(tokens).save(token);
        verify(tokens).save(second);
    }
}
