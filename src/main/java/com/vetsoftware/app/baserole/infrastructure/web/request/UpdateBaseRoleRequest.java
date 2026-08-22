package com.vetsoftware.app.baserole.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateBaseRoleRequest(
        @NotBlank(message = "El nombre del rol base es obligatorio.") @Size(max = 100, message = "El nombre del rol base no puede superar los 100 caracteres.") String name,
        @NotBlank(message = "El código del rol base es obligatorio.") @Size(max = 50, message = "El código del rol base no puede superar los 50 caracteres.") String code,
        @NotNull(message = "Debes indicar si el rol base es obligatorio.") Boolean mandatory) {
}
