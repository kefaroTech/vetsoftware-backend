package com.vetsoftware.app.employeebranch.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record EmployeeBranchesResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long employeeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Long> branchIds) {
}
