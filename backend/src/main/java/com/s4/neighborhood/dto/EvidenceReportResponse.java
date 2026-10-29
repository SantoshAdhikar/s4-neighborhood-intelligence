package com.s4.neighborhood.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EvidenceReportResponse(
        UUID id,
        UUID businessId,
        String status,
        String modelName,
        String modelResponseId,
        List<Source> sources,
        List<Opportunity> opportunities,
        List<DayAction> plan,
        List<String> limitations,
        String lastError,
        Instant createdAt,
        Instant completedAt
) {
    public record Source(String id, String kind, String description, String sourceUrl, Instant observedAt) {}
    public record Opportunity(int rank, String title, String rationale, List<String> evidenceIds,
                              String uncertainty, String effort, String measurement) {}
    public record DayAction(int day, String task, String purpose, List<String> evidenceIds, String measurement) {}
    public record Draft(List<Opportunity> opportunities, List<DayAction> plan, List<String> limitations) {}
}
