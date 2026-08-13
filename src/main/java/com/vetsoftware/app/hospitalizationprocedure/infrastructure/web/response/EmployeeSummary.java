package com.vetsoftware.app.hospitalizationprocedure.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record EmployeeSummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String employeeCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {
}
