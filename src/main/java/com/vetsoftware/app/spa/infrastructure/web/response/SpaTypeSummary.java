package com.vetsoftware.app.spa.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SpaTypeSummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {
}
