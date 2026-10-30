package com.s4.neighborhood.service;

import com.s4.neighborhood.entity.AppUser;
import com.s4.neighborhood.entity.PasswordResetToken;
import com.s4.neighborhood.repository.AppUserRepository;
import com.s4.neighborhood.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration EXPIRY = Duration.ofMinutes(30);
    private static final Duration RESEND_WINDOW = Duration.ofMinutes(2);

    private final AppUserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwords;
    private final ObjectProvider<JavaMailSender> mail;
    private final boolean mailEnabled;
    private final String from;
    private final String frontendUrl;
    private final Clock clock;

    @Autowired
    public PasswordResetService(AppUserRepository users, PasswordResetTokenRepository tokens,
                                PasswordEncoder passwords, ObjectProvider<JavaMailSender> mail,
                                @Value("${s4.verification.email-enabled:false}") boolean mailEnabled,
                                @Value("${s4.verification.from:}") String from,
                                @Value("${s4.verification.frontend-url:http://localhost:5173}") String frontendUrl) {
        this(users, tokens, passwords, mail, mailEnabled, from, frontendUrl, Clock.systemUTC());
    }

    PasswordResetService(AppUserRepository users, PasswordResetTokenRepository tokens,
                         PasswordEncoder passwords, ObjectProvider<JavaMailSender> mail,
                         boolean mailEnabled, String from, String frontendUrl, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.passwords = passwords;
        this.mail = mail;
        this.mailEnabled = mailEnabled;
        this.from = from;
        this.frontendUrl = frontendUrl;
        this.clock = clock;
    }

    /** Same public response for known and unknown addresses. Never log reset links. */
    @Transactional
    public void request(String address) {
        if (!mailEnabled || from.isBlank() || mail.getIfAvailable() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Password reset email is not configured");
        }
        var existing = users.findByEmail(address.trim().toLowerCase(Locale.ROOT));
        if (existing.isEmpty()) return;
        AppUser user = existing.get();
        Instant now = clock.instant();
        var recent = tokens.findTopByUserIdOrderByCreatedAtDesc(user.getId());
        if (recent.isPresent() && recent.get().getCreatedAt().isAfter(now.minus(RESEND_WINDOW))) return;

        byte[] entropy = new byte[32];
        RANDOM.nextBytes(entropy);
        String rawToken = HexFormat.of().formatHex(entropy);
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(EXPIRY));
        token.setTokenSha256(EmailVerificationService.sha256(rawToken));
        tokens.saveAndFlush(token);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("Reset your S4 Neighborhood password");
        message.setText("You requested a password reset. This one-time link expires in 30 minutes:\n"
                + frontendUrl.replaceAll("/+$", "") + "/reset-password?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8)
                + "\n\nIf you didn't request this, you can ignore this email.");
        try {
            mail.getObject().send(message);
        } catch (RuntimeException exception) {
            log.warn("Password reset delivery failed for account {}: {}", user.getId(), exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Password reset email unavailable");
        }
    }

    @Transactional
    public void confirm(String rawToken, String newPassword) {
        if (rawToken == null || !rawToken.matches("[a-f0-9]{64}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset link");
        }
        PasswordResetToken token = tokens.findByTokenSha256(EmailVerificationService.sha256(rawToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset link"));
        Instant now = clock.instant();
        if (token.getConsumedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset link");
        }
        AppUser user = users.findById(token.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset link"));
        user.setPasswordHash(passwords.encode(newPassword));
        users.save(user);
        // Revoke other unused reset links for this account after password change.
        for (PasswordResetToken sibling : tokens.findByUserIdAndConsumedAtIsNull(user.getId())) {
            sibling.setConsumedAt(now);
            tokens.save(sibling);
        }
    }
}
