package com.vetsoftware.app.auth.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MeResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String type, Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name, String employeeCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean mustChangePassword,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> permissions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Long> branchIds) {
}
