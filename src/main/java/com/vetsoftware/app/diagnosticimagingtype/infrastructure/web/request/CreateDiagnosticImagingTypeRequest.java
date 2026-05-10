package com.vetsoftware.app.diagnosticimagingtype.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDiagnosticImagingTypeRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 500) String description,
        Long companyId,
        boolean general
) {}
