package com.s4.neighborhood.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analysis_runs")
public class AnalysisRun {
    @Id
    private UUID id = UUID.randomUUID();
    @Column(name = "business_id", nullable = false)
    private UUID businessId;
    @Column(nullable = false, length = 20)
    private String status = "QUEUED";
    @Column(nullable = false, length = 40)
    private String step = "QUEUED";
    @Column(length = 500)
    private String detail;
    @Column(nullable = false, length = 5000)
    private String goal;
    @Column(length = 500)
    private String website;
    @Column(name = "focus_services", length = 500)
    private String focusServices;
    @Column(name = "customer_feedback", length = 1500)
    private String customerFeedback;
    @Column(name = "media_id")
    private UUID mediaId;
    @Column(name = "report_id")
    private UUID reportId;
    @Column(name = "use_meta", nullable = false)
    private boolean useMeta;
    @Column(name = "segment_sign", nullable = false)
    private boolean segmentSign;
    @Column(name = "last_error", length = 500)
    private String lastError;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    @Column(name = "completed_at")
    private Instant completedAt;

    @PreUpdate
    void updateTimestamp() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getBusinessId() { return businessId; }
    public void setBusinessId(UUID v) { businessId=v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status=v; }
    public String getStep() { return step; }
    public void setStep(String v) { step=v; }
    public String getDetail() { return detail; }
    public void setDetail(String v) { detail=v; }
    public String getGoal() { return goal; }
    public void setGoal(String v) { goal=v; }
    public String getWebsite() { return website; }
    public void setWebsite(String v) { website=v; }
    public String getFocusServices() { return focusServices; }
    public void setFocusServices(String v) { focusServices=v; }
    public String getCustomerFeedback() { return customerFeedback; }
    public void setCustomerFeedback(String v) { customerFeedback=v; }
    public UUID getMediaId() { return mediaId; }
    public void setMediaId(UUID v) { mediaId=v; }
    public UUID getReportId() { return reportId; }
    public void setReportId(UUID v) { reportId=v; }
    public boolean isUseMeta() { return useMeta; }
    public void setUseMeta(boolean v) { useMeta=v; }
    public boolean isSegmentSign() { return segmentSign; }
    public void setSegmentSign(boolean v) { segmentSign=v; }
    public String getLastError() { return lastError; }
    public void setLastError(String v) { lastError=v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant v) { completedAt=v; }
}
