
package com.s4.neighborhood.dto;

import jakarta.validation.constraints.*;

public record BusinessProfileRequest(
        @NotBlank @Size(max = 160)
        String name,

        @NotBlank @Size(max = 120)
        String industry,

        @NotBlank @Size(max = 250)
        String streetAddress,

        @NotBlank @Size(max = 120)
        String city,

        @NotBlank @Size(max = 100)
        String state,

        @NotBlank @Size(max = 20)
        String postalCode,

        @Size(max = 500)
        @Pattern(
                regexp = "^$|https?://[^\\s]+",
                message = "Website must begin with http:// or https://"
        )
        String website,

        @NotBlank
        String services,

        @NotBlank @Size(max = 1000)
        String primaryGoal,

        @NotNull
        Integer researchRadiusMiles
) {}
