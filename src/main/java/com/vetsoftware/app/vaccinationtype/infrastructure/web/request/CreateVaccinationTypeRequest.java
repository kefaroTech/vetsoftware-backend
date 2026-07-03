package com.vetsoftware.app.vaccinationtype.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVaccinationTypeRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        boolean general
) {}
