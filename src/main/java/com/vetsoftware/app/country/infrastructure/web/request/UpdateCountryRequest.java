package com.vetsoftware.app.country.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCountryRequest(
        @NotBlank(message = "El nombre del país es obligatorio.") @Size(max = 100, message = "El nombre del país no puede superar los 100 caracteres.") String name) {
}
