package com.vetsoftware.app.hospitalizationprogressnote.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateHospitalizationProgressNoteRequest(
        @NotBlank(message = "La nota de evolución es obligatoria.") @Size(max = 2000, message = "La nota de evolución no puede superar los 2000 caracteres.") String description,
        @NotNull(message = "Debes seleccionar la hospitalización.") Long hospitalizationId) {
}
