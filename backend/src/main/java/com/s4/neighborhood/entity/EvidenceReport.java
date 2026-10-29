package com.s4.neighborhood.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence_reports")
public class EvidenceReport {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "source_fingerprint", nullable = false, length = 64)
    private String sourceFingerprint;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(nullable = false)
    private int attempts;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "model_response_id", length = 150)
    private String modelResponseId;

    @Column(name = "report_json", columnDefinition = "text")
    private String reportJson;

    @Column(name = "last_error", length = 400)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void initialize() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getBusinessId() { return businessId; }
    public void setBusinessId(UUID value) { businessId = value; }
    public String getSourceFingerprint() { return sourceFingerprint; }
    public void setSourceFingerprint(String value) { sourceFingerprint = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public int getAttempts() { return attempts; }
    public String getModelName() { return modelName; }
    public void setModelName(String value) { modelName = value; }
    public String getModelResponseId() { return modelResponseId; }
    public void setModelResponseId(String value) { modelResponseId = value; }
    public String getReportJson() { return reportJson; }
    public void setReportJson(String value) { reportJson = value; }
    public String getLastError() { return lastError; }
    public void setLastError(String value) { lastError = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant value) { completedAt = value; }
}
