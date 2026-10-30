package com.s4.neighborhood.dto;

import java.time.Instant;

public record BusinessClaimResponse(String status, String explanation, Instant requestedAt) {}
