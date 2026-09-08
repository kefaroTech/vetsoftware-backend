package com.vetsoftware.app.diagnosticimagingtype.infrastructure.web.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDiagnosticImagingTypeRequest(
        @NotBlank(message = "El nombre del tipo de imagen diagnóstica es obligatorio.") @Size(max = 100, message = "El nombre del tipo de imagen diagnóstica no puede superar los 100 caracteres.") String name,
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.") String description,
        // Jackson 3: sin @JsonSetter, omitir el campo responde 400 en vez de caer a
        // false (ver CreateAppointmentRequest.forceOverlap).
        @JsonSetter(nulls = Nulls.AS_EMPTY) boolean general) {
}
