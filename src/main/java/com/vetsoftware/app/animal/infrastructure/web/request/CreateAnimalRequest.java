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

public record CreateAnimalRequest(
        @NotBlank(message = "El nombre de la mascota es obligatorio.") @Size(max = 100, message = "El nombre de la mascota no puede superar los 100 caracteres.") String name,
        @Size(max = 50, message = "El código de la mascota no puede superar los 50 caracteres.") String code,
        @NotNull(message = "Debes seleccionar la especie.") Long specieId,
        @NotNull(message = "Debes seleccionar la raza.") Long breedId,
        @NotNull(message = "Debes seleccionar el propietario.") Long ownerId,
        @NotNull(message = "Debes seleccionar el sexo.") Gender gender,
        @NotNull(message = "Debes seleccionar la unidad de peso.") WeightType weightType,
        @NotNull(message = "Debes seleccionar el tipo de mascota.") AnimalType animalType,
        @NotNull(message = "Debes seleccionar el estado reproductivo.") ReproductiveState reproductiveState,
        @NotNull(message = "Debes seleccionar el color.") Long colorId, LocalDate bod,
        // Peso inicial opcional: si viene, se registra como primer WeightRecord
        // (source=MANUAL) en la
        // unidad weightType. El peso posterior se gestiona vía
        // /animals/{id}/weight-records.
        @Positive(message = "El peso debe ser mayor que cero.") BigDecimal weight,
        @PositiveOrZero(message = "La talla no puede ser negativa.") Integer size, boolean deceased,
        LocalDate deceasedDate) {
}
