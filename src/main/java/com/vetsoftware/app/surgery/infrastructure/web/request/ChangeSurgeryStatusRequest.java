package com.vetsoftware.app.surgery.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeSurgeryStatusRequest(
        @NotBlank String status
) {}
