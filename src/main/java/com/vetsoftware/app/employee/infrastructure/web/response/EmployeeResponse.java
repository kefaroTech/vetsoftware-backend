package com.vetsoftware.app.employee.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record EmployeeResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String employeeCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<EmployeeRoleSummary> roles,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<BranchSummary> branches,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean mustChangePassword,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status) {
}
