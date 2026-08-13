package com.vetsoftware.app.systemuserpermission.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record SystemUserPermissionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SystemUserSummary systemUser,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SystemPermissionSummary systemPermission,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
