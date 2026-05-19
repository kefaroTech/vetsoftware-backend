package com.vetsoftware.app.diagnosticimaging.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeDiagnosticImagingStatusRequest(
        @NotBlank String status
) {}
