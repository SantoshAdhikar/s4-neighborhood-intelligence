package com.s4.neighborhood.repository;

import com.s4.neighborhood.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerificationToken> findByTokenSha256(String hash);
    Optional<EmailVerificationToken> findTopByUserIdOrderByCreatedAtDesc(UUID userId);
}
