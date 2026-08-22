package com.vetsoftware.app.laboratorytest.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeLaboratoryTestStatusRequest(
        @NotBlank(message = "Debes seleccionar el estado del examen.") String status) {
}
