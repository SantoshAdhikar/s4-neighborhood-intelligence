package com.s4.neighborhood.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "website_findings")
public class WebsiteFinding {
    @Id
    private UUID id = UUID.randomUUID();
    @Column(name = "run_id", nullable = false)
    private UUID runId;
    @Column(name = "business_id", nullable = false)
    private UUID businessId;
    @Column(nullable = false, length = 24)
    private String kind;
    @Column(name = "business_name", nullable = false, length = 160)
    private String businessName;
    @Column(nullable = false, length = 500)
    private String url;
    @Column(length = 250)
    private String title;
    @Column(name = "observed_summary", length = 1200)
    private String observedSummary;
    @Column(nullable = false, length = 24)
    private String status;
    @Column(length = 350)
    private String note;
    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt = Instant.now();

    public UUID getId() { return id; }
    public UUID getRunId() { return runId; }
    public void setRunId(UUID v) { runId=v; }
    public UUID getBusinessId() { return businessId; }
    public void setBusinessId(UUID v) { businessId=v; }
    public String getKind() { return kind; }
    public void setKind(String v) { kind=v; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String v) { businessName=v; }
    public String getUrl() { return url; }
    public void setUrl(String v) { url=v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title=v; }
    public String getObservedSummary() { return observedSummary; }
    public void setObservedSummary(String v) { observedSummary=v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status=v; }
    public String getNote() { return note; }
    public void setNote(String v) { note=v; }
    public Instant getFetchedAt() { return fetchedAt; }
}
