package com.s4.neighborhood.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "business_claims")
public class BusinessClaim {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "business_id", nullable = false, unique = true)
    private UUID businessId;
    @Column(name = "claimant_user_id", nullable = false)
    private UUID claimantUserId;
    @Column(name = "identity_sha256", nullable = false, length = 64)
    private String identitySha256;
    @Column(nullable = false, length = 24)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    public UUID getId() { return id; }
    public UUID getBusinessId() { return businessId; }
    public void setBusinessId(UUID id) { businessId = id; }
    public UUID getClaimantUserId() { return claimantUserId; }
    public void setClaimantUserId(UUID id) { claimantUserId = id; }
    public String getIdentitySha256() { return identitySha256; }
    public void setIdentitySha256(String value) { identitySha256 = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { updatedAt = value; }
}
