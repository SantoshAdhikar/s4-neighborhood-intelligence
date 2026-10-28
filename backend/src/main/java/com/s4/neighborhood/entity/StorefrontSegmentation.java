package com.s4.neighborhood.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "storefront_segmentations")
public class StorefrontSegmentation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "media_id", nullable = false)
    private UUID mediaId;

    @Column(nullable = false, length = 80)
    private String concept;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(nullable = false)
    private int attempts;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "model_response_id", length = 150)
    private String modelResponseId;

    @Column(name = "raw_output", columnDefinition = "text")
    private String rawOutput;

    @Column(name = "last_error", length = 400)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void initialize() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getMediaId() { return mediaId; }
    public void setMediaId(UUID value) { mediaId = value; }
    public String getConcept() { return concept; }
    public void setConcept(String value) { concept = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int value) { attempts = value; }
    public String getModelName() { return modelName; }
    public void setModelName(String value) { modelName = value; }
    public String getModelResponseId() { return modelResponseId; }
    public void setModelResponseId(String value) { modelResponseId = value; }
    public String getRawOutput() { return rawOutput; }
    public void setRawOutput(String value) { rawOutput = value; }
    public String getLastError() { return lastError; }
    public void setLastError(String value) { lastError = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant value) { completedAt = value; }
}
