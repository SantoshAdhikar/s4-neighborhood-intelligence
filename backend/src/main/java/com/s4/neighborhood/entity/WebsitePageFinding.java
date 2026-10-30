package com.s4.neighborhood.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "website_page_findings")
public class WebsitePageFinding {
    @Id
    private UUID id = UUID.randomUUID();
    @Column(name = "run_id", nullable = false)
    private UUID runId;
    @Column(name = "business_id", nullable = false)
    private UUID businessId;
    @Column(name = "website_finding_id", nullable = false)
    private UUID websiteFindingId;
    @Column(name = "page_type", nullable = false, length = 24)
    private String pageType;
    @Column(name = "page_url", nullable = false, length = 500)
    private String pageUrl;
    @Column(name = "page_title", length = 250)
    private String pageTitle;
    @Column(nullable = false, length = 24)
    private String status;
    @Column(name = "observed_summary", length = 2000)
    private String observedSummary;
    @Column(length = 350)
    private String note;
    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt = Instant.now();

    public UUID getId() { return id; }
    public UUID getRunId() { return runId; }
    public void setRunId(UUID value) { runId = value; }
    public UUID getBusinessId() { return businessId; }
    public void setBusinessId(UUID value) { businessId = value; }
    public UUID getWebsiteFindingId() { return websiteFindingId; }
    public void setWebsiteFindingId(UUID value) { websiteFindingId = value; }
    public String getPageType() { return pageType; }
    public void setPageType(String value) { pageType = value; }
    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String value) { pageUrl = value; }
    public String getPageTitle() { return pageTitle; }
    public void setPageTitle(String value) { pageTitle = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getObservedSummary() { return observedSummary; }
    public void setObservedSummary(String value) { observedSummary = value; }
    public String getNote() { return note; }
    public void setNote(String value) { note = value; }
    public Instant getFetchedAt() { return fetchedAt; }
}
