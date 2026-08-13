package com.vetsoftware.app.employee.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record EmployeeCodeAvailabilityResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean available) {
}
