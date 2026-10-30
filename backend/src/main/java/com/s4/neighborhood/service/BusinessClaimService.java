package com.s4.neighborhood.service;

import com.s4.neighborhood.dto.BusinessClaimResponse;
import com.s4.neighborhood.entity.AppUser;
import com.s4.neighborhood.entity.BusinessClaim;
import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.repository.AppUserRepository;
import com.s4.neighborhood.repository.BusinessClaimRepository;
import com.s4.neighborhood.repository.BusinessProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class BusinessClaimService {
    private static final Pattern SYMBOLS = Pattern.compile("[^\\p{L}\\p{N}]+");
    private final BusinessClaimRepository claims;
    private final BusinessProfileRepository businesses;
    private final AppUserRepository users;
    public BusinessClaimService(BusinessClaimRepository claims, BusinessProfileRepository businesses,
                                AppUserRepository users) {
        this.claims = claims;
        this.businesses = businesses;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public BusinessClaimResponse status(UUID businessId) {
        BusinessProfile business = businesses.findById(businessId).orElseThrow();
        return claims.findByBusinessId(businessId)
                .map(claim -> {
                    if (!claim.getIdentitySha256().equals(identity(business))) {
                        return new BusinessClaimResponse("NEEDS_RESUBMISSION",
                                "Business identity changed. Request review again.", claim.getCreatedAt());
                    }
                    return response(claim);
                })
                .orElseGet(() -> new BusinessClaimResponse("UNVERIFIED",
                        "No ownership claim submitted. This is a private research workspace.", null));
    }

    @Transactional
    public BusinessClaimResponse requestReview(UUID businessId, UUID currentUserId) {
        AppUser claimant = users.findById(currentUserId).orElseThrow();
        if (!claimant.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Verify your account email before requesting business ownership review");
        }
        BusinessProfile business = businesses.findById(businessId).orElseThrow();
        if (!currentUserId.equals(business.getOwnerUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found");
        }
        String key = identity(business);
        BusinessClaim claim = claims.findByBusinessId(businessId).orElse(null);
        if (claim != null && "VERIFIED".equals(claim.getStatus())) return response(claim);
        if (claim == null) {
            claim = new BusinessClaim();
            claim.setBusinessId(businessId);
            claim.setClaimantUserId(currentUserId);
            claim.setCreatedAt(Instant.now());
        }
        claim.setIdentitySha256(key);
        var otherClaims = claims.findByIdentitySha256(key).stream()
                .filter(other -> !other.getBusinessId().equals(businessId))
                .filter(other -> !"REJECTED".equals(other.getStatus()))
                .toList();
        boolean conflict = !otherClaims.isEmpty();
        for (BusinessClaim other : otherClaims) {
            if (!"VERIFIED".equals(other.getStatus())) {
                other.setStatus("CONFLICT_REVIEW");
                other.setUpdatedAt(Instant.now());
                claims.save(other);
            }
        }
        claim.setStatus(conflict ? "CONFLICT_REVIEW" : "PENDING_REVIEW");
        claim.setUpdatedAt(Instant.now());
        return response(claims.saveAndFlush(claim));
    }

    static String identity(BusinessProfile business) {
        String stable = normalize(business.getName()) + "|" + normalize(business.getStreetAddress())
                + "|" + normalize(business.getCity()) + "|" + normalize(business.getState())
                + "|" + normalize(business.getPostalCode());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(stable.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Identity hashing unavailable", exception);
        }
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return SYMBOLS.matcher(value.toLowerCase(Locale.ROOT)).replaceAll("").trim();
    }

    private static BusinessClaimResponse response(BusinessClaim claim) {
        String explanation = "CONFLICT_REVIEW".equals(claim.getStatus())
                ? "Potential duplicate claim detected. Both claims require independent review. No ownership was transferred."
                : "Ownership review requested. This does not verify legal ownership or grant access to other accounts.";
        return new BusinessClaimResponse(claim.getStatus(), explanation, claim.getCreatedAt());
    }
}
