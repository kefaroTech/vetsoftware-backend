package com.vetsoftware.app.animal.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.animal.domain.AnimalType;
import com.vetsoftware.app.animal.domain.Gender;
import com.vetsoftware.app.animal.domain.ReproductiveState;
import com.vetsoftware.app.animal.domain.WeightType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AnimalResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name, String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SpecieSummary specie,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BreedSummary breed,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OwnerSummary owner,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Gender gender,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) WeightType weightType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AnimalType animalType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ReproductiveState reproductiveState,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AnimalColorSummary color,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate bod, BigDecimal weight,
        LocalDate weightMeasuredAt, Integer size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean deceased,
        LocalDate deceasedDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
