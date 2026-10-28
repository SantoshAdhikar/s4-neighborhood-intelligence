package com.s4.neighborhood.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "storefront_media")
public class StorefrontMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "original_filename", nullable = false, length = 200)
    private String originalFilename;

    @Column(name = "storage_key", nullable = false, length = 80)
    private String storageKey;

    @Column(name = "mime_type", nullable = false, length = 30)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(nullable = false, length = 20)
    private String status = "UPLOADED";

    @Column(name = "analysis_attempts", nullable = false)
    private int analysisAttempts;

    @Column(name = "analysis_json", columnDefinition = "text")
    private String analysisJson;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "model_response_id", length = 150)
    private String modelResponseId;

    @Column(name = "last_error", length = 400)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "analyzed_at")
    private Instant analyzedAt;

    @PrePersist
    void initialize() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getBusinessId() { return businessId; }
    public void setBusinessId(UUID value) { businessId = value; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String value) { originalFilename = value; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String value) { storageKey = value; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String value) { mimeType = value; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long value) { sizeBytes = value; }
    public String getSha256() { return sha256; }
    public void setSha256(String value) { sha256 = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public int getAnalysisAttempts() { return analysisAttempts; }
    public void setAnalysisAttempts(int value) { analysisAttempts = value; }
    public String getAnalysisJson() { return analysisJson; }
    public void setAnalysisJson(String value) { analysisJson = value; }
    public String getModelName() { return modelName; }
    public void setModelName(String value) { modelName = value; }
    public String getModelResponseId() { return modelResponseId; }
    public void setModelResponseId(String value) { modelResponseId = value; }
    public String getLastError() { return lastError; }
    public void setLastError(String value) { lastError = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(Instant value) { analyzedAt = value; }
}
