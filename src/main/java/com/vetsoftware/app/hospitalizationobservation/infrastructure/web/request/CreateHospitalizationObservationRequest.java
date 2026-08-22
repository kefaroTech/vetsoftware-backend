package com.vetsoftware.app.hospitalizationobservation.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateHospitalizationObservationRequest(
        @NotBlank(message = "La observación es obligatoria.") @Size(max = 2000, message = "La observación no puede superar los 2000 caracteres.") String description,
        @NotNull(message = "Debes seleccionar la hospitalización.") Long hospitalizationId) {
}
