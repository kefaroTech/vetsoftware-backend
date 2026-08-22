package com.vetsoftware.app.module.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateModuleRequest(
        @NotBlank(message = "El nombre del módulo es obligatorio.") @Size(max = 100, message = "El nombre del módulo no puede superar los 100 caracteres.") String name,
        @NotBlank(message = "El código del módulo es obligatorio.") @Size(max = 50, message = "El código del módulo no puede superar los 50 caracteres.") String code) {
}
