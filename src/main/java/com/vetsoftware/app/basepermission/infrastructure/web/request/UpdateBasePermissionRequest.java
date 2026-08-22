package com.vetsoftware.app.basepermission.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateBasePermissionRequest(
        @NotBlank(message = "El nombre del permiso base es obligatorio.") @Size(max = 100, message = "El nombre del permiso base no puede superar los 100 caracteres.") String name,
        @NotBlank(message = "El código del permiso base es obligatorio.") @Size(max = 50, message = "El código del permiso base no puede superar los 50 caracteres.") String code,
        @NotNull(message = "Debes seleccionar el submódulo.") Long subModuleId) {
}
