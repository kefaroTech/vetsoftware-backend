package com.vetsoftware.app.animal.infrastructure.web.request;

import com.vetsoftware.app.animal.domain.AnimalType;
import com.vetsoftware.app.animal.domain.Gender;
import com.vetsoftware.app.animal.domain.ReproductiveState;
import com.vetsoftware.app.animal.domain.WeightType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateAnimalRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 50) String code,
        @NotNull Long specieId,
        @NotNull Long breedId,
        @NotNull Long ownerId,
        @NotNull Gender gender,
        @NotNull WeightType weightType,
        @NotNull AnimalType animalType,
        @NotNull ReproductiveState reproductiveState,
        @NotNull Long colorId,
        LocalDate bod,
        // Ignorado: el peso ya no se edita desde el animal; se gestiona vía /animals/{id}/weight-records.
        @Positive BigDecimal weight,
        @PositiveOrZero Integer size,
        boolean deceased,
        LocalDate deceasedDate,
        @NotNull Long companyId
) {}
