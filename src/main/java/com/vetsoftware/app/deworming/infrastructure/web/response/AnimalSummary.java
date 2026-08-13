package com.vetsoftware.app.deworming.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AnimalSummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name, String code) {
}
