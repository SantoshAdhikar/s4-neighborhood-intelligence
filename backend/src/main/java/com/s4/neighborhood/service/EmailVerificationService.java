package com.s4.neighborhood.service;

import com.s4.neighborhood.entity.AppUser;
import com.s4.neighborhood.entity.EmailVerificationToken;
import com.s4.neighborhood.repository.AppUserRepository;
import com.s4.neighborhood.repository.EmailVerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class EmailVerificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration EXPIRY = Duration.ofHours(24);
    private final AppUserRepository users;
    private final EmailVerificationTokenRepository tokens;
    private final ObjectProvider<JavaMailSender> senders;
    private final boolean enabled;
    private final String senderAddress;
    private final String frontendUrl;
    private final Clock clock;

    @Autowired
    public EmailVerificationService(AppUserRepository users, EmailVerificationTokenRepository tokens,
                                    ObjectProvider<JavaMailSender> senders,
                                    @Value("${s4.verification.email-enabled:false}") boolean enabled,
                                    @Value("${s4.verification.from:}") String senderAddress,
                                    @Value("${s4.verification.frontend-url:http://localhost:5173}") String frontendUrl) {
        this(users, tokens, senders, enabled, senderAddress, frontendUrl, Clock.systemUTC());
    }

    EmailVerificationService(AppUserRepository users, EmailVerificationTokenRepository tokens,
                             ObjectProvider<JavaMailSender> senders, boolean enabled, String senderAddress,
                             String frontendUrl, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.senders = senders;
        this.enabled = enabled;
        this.senderAddress = senderAddress;
        this.frontendUrl = frontendUrl;
        this.clock = clock;
    }

    public boolean isAvailable() {
        return enabled && !senderAddress.isBlank() && senders.getIfAvailable() != null;
    }

    /** Never return or log the raw token; only the user receives it by email. */
    @Transactional
    public boolean sendIfConfigured(String email) {
        if (!isAvailable()) return false;
        AppUser user = users.findByEmail(email).orElseThrow();
        if (user.isEmailVerified()) return true;
        Instant now = clock.instant();
        var latest = tokens.findTopByUserIdOrderByCreatedAtDesc(user.getId());
        if (latest.isPresent() && latest.get().getCreatedAt().isAfter(now.minusSeconds(60))) {
            return true; // Throttle repeated sends without revealing account state.
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = HexFormat.of().formatHex(bytes);
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId(user.getId());
        token.setTokenSha256(sha256(raw));
        token.setExpiresAt(now.plus(EXPIRY));
        token.setCreatedAt(now);
        tokens.saveAndFlush(token);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(user.getEmail());
        message.setSubject("Verify your S4 Neighborhood account");
        message.setText("Confirm your email address using this one-time link (expires in 24 hours):\n"
                + frontendUrl.replaceAll("/+$", "") + "/verify-email?token="
                + URLEncoder.encode(raw, StandardCharsets.UTF_8)
                + "\n\nThis only verifies email access, not ownership of any business.");
        try {
            senders.getObject().send(message);
        } catch (RuntimeException exception) {
            log.warn("Verification email delivery failed for account {}: {}",
                    user.getId(), exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Verification email could not be sent. Check SMTP settings.");
        }
        return true;
    }

    @Transactional
    public void confirm(String rawToken) {
        if (rawToken == null || !rawToken.matches("[a-f0-9]{64}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification link");
        }
        EmailVerificationToken token = tokens.findByTokenSha256(sha256(rawToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid or expired verification link"));
        if (token.getConsumedAt() != null || !token.getExpiresAt().isAfter(clock.instant())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired verification link");
        }
        AppUser user = users.findById(token.getUserId()).orElseThrow();
        user.setEmailVerified(true);
        token.setConsumedAt(clock.instant());
        users.save(user);
        tokens.save(token);
    }

    static String sha256(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
