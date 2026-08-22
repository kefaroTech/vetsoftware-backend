package com.vetsoftware.app.role.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
        @NotBlank(message = "El nombre del rol es obligatorio.") @Size(max = 100, message = "El nombre del rol no puede superar los 100 caracteres.") String name,
        @NotBlank(message = "El código del rol es obligatorio.") @Size(max = 50, message = "El código del rol no puede superar los 50 caracteres.") String code) {
}
