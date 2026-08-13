package com.vetsoftware.app.systemuserpermission.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SystemUserSummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code) {
}
