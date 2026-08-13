package com.vetsoftware.app.permission.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SubModuleSummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code) {
}
