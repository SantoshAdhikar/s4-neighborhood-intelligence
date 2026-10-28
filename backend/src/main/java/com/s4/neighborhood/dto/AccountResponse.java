package com.s4.neighborhood.dto;

import java.util.UUID;

public record AccountResponse(UUID id, String email, String displayName) {}
