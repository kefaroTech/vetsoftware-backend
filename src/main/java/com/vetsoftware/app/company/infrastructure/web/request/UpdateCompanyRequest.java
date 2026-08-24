package com.vetsoftware.app.company.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
        @NotBlank(message = "El nombre de la empresa es obligatorio.") @Size(max = 100, message = "El nombre de la empresa no puede superar los 100 caracteres.") String name,
        @NotBlank(message = "El número de identificación de la empresa es obligatorio.") @Size(max = 50, message = "El número de identificación de la empresa no puede superar los 50 caracteres.") String identifier,
        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres.") String address,
        @Size(max = 30, message = "El teléfono de contacto no puede superar los 30 caracteres.") String contactNumber,
        @NotNull(message = "Debes seleccionar la ciudad.") Long cityId) {
}
