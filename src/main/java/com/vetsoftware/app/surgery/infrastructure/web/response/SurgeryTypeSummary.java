package com.vetsoftware.app.surgery.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SurgeryTypeSummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {
}
