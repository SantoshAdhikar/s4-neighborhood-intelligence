package com.s4.neighborhood.repository;

import com.s4.neighborhood.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByTokenSha256(String hash);
    List<PasswordResetToken> findByUserIdAndConsumedAtIsNull(UUID userId);
    Optional<PasswordResetToken> findTopByUserIdOrderByCreatedAtDesc(UUID userId);
}
