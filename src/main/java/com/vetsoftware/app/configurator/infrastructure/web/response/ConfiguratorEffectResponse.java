package com.vetsoftware.app.configurator.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ConfiguratorEffectResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id, Long optionId,
        Long questionId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long catalogItemId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String effect, Integer quantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
