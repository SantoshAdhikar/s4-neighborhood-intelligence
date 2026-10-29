package com.s4.neighborhood.controller;

import com.s4.neighborhood.dto.AnalysisRunRequest;
import com.s4.neighborhood.dto.AnalysisRunResponse;
import com.s4.neighborhood.security.BusinessAccessGuard;
import com.s4.neighborhood.service.AnalysisRunService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/analysis-runs")
public class AnalysisRunController {
    private final BusinessAccessGuard access;
    private final AnalysisRunService runs;
    public AnalysisRunController(BusinessAccessGuard access, AnalysisRunService runs) {
        this.access = access;
        this.runs = runs;
    }
    @PostMapping
    public ResponseEntity<AnalysisRunResponse> start(@PathVariable UUID businessId,
                                                     @Valid @RequestBody AnalysisRunRequest request) {
        access.requireOwner(businessId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(runs.start(businessId, request));
    }
    @GetMapping("/latest")
    public ResponseEntity<AnalysisRunResponse> latest(@PathVariable UUID businessId) {
        access.requireOwner(businessId);
        return runs.latest(businessId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
    @GetMapping("/{runId}")
    public AnalysisRunResponse get(@PathVariable UUID businessId, @PathVariable UUID runId) {
        access.requireOwner(businessId);
        return runs.get(businessId, runId);
    }
}
