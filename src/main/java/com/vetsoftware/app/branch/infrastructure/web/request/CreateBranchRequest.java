package com.vetsoftware.app.branch.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBranchRequest(
        @NotBlank(message = "El nombre de la sede es obligatorio.") @Size(max = 120, message = "El nombre de la sede no puede superar los 120 caracteres.") String name,
        @NotBlank(message = "El código de la sede es obligatorio.") @Size(max = 30, message = "El código de la sede no puede superar los 30 caracteres.") String code,
        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres.") String address,
        @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres.") String phone,
        @NotNull(message = "Debes seleccionar la ciudad.") Long cityId) {
}
