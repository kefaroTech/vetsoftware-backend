package com.vetsoftware.app.submodule.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateSubModuleRequest(
        @NotBlank(message = "El nombre del submódulo es obligatorio.") @Size(max = 100, message = "El nombre del submódulo no puede superar los 100 caracteres.") String name,
        @NotBlank(message = "El código del submódulo es obligatorio.") @Size(max = 50, message = "El código del submódulo no puede superar los 50 caracteres.") String code,
        @NotNull(message = "Debes seleccionar el módulo.") Long moduleId) {
}
