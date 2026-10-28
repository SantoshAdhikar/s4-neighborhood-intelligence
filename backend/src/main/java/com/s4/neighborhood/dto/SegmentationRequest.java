package com.s4.neighborhood.dto;

import jakarta.validation.constraints.NotBlank;

public record SegmentationRequest(@NotBlank String concept) {}
