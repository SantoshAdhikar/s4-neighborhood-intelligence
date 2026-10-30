package com.s4.neighborhood.controller;

import com.s4.neighborhood.dto.AnalysisRunRequest;
import com.s4.neighborhood.dto.AnalysisRunResponse;
import com.s4.neighborhood.dto.AnalysisRunHistoryResponse;
import com.s4.neighborhood.security.BusinessAccessGuard;
import com.s4.neighborhood.service.AnalysisRunService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.List;

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
    @GetMapping
    public AnalysisRunHistoryResponse history(@PathVariable UUID businessId,
                                              @RequestParam(defaultValue = "0") int page) {
        access.requireOwner(businessId);
        return runs.history(businessId, page);
    }
    @GetMapping("/latest")
    public ResponseEntity<AnalysisRunResponse> latest(@PathVariable UUID businessId) {
        access.requireOwner(businessId);
        return runs.latest(businessId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
    public record DeleteHistoryRequest(List<UUID> ids) {}

    @PostMapping("/delete")
    public ResponseEntity<Void> deleteMany(@PathVariable UUID businessId,
                                           @RequestBody DeleteHistoryRequest request) {
        access.requireOwner(businessId);
        runs.deleteHistory(businessId, request.ids());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{runId}")
    public ResponseEntity<Void> deleteOne(@PathVariable UUID businessId, @PathVariable UUID runId) {
        access.requireOwner(businessId);
        runs.deleteHistory(businessId, List.of(runId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{runId}")
    public AnalysisRunResponse get(@PathVariable UUID businessId, @PathVariable UUID runId) {
        access.requireOwner(businessId);
        return runs.get(businessId, runId);
    }
}
