package com.s4.neighborhood.controller;

import com.s4.neighborhood.dto.EvidenceReportResponse;
import com.s4.neighborhood.security.BusinessAccessGuard;
import com.s4.neighborhood.service.EvidenceReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/evidence-report")
public class EvidenceReportController {
    private final BusinessAccessGuard access;
    private final EvidenceReportService reports;

    public EvidenceReportController(BusinessAccessGuard access, EvidenceReportService reports) {
        this.access = access;
        this.reports = reports;
    }

    @GetMapping
    public ResponseEntity<EvidenceReportResponse> latest(@PathVariable UUID businessId) {
        access.requireOwner(businessId);
        return reports.latest(businessId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public EvidenceReportResponse generate(@PathVariable UUID businessId) {
        access.requireOwner(businessId);
        return reports.generate(businessId);
    }
}
