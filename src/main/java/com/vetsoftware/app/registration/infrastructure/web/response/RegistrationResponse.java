package com.vetsoftware.app.registration.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record RegistrationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long employeeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status) {
}
