package com.vetsoftware.app.animal.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.animal.domain.WeightSource;
import com.vetsoftware.app.animal.domain.WeightType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WeightRecordResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long animalId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String animalName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String animalCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal value,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) WeightType unit,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate measuredAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) WeightSource source, Long sourceId,
        String note,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {
}
