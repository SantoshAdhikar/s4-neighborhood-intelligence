package com.s4.neighborhood.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailVerificationRequest(@NotBlank @Size(max = 150) String token) {}
