package com.vetsoftware.app.configurator.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SelectedItemResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long catalogItemId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int quantity) {
}
