package com.vetsoftware.app.systempermission.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSystemPermissionRequest(
        @NotBlank(message = "El nombre del permiso de sistema es obligatorio.") @Size(max = 100, message = "El nombre del permiso de sistema no puede superar los 100 caracteres.") String name,
        @NotBlank(message = "El código del permiso de sistema es obligatorio.") @Size(max = 50, message = "El código del permiso de sistema no puede superar los 50 caracteres.") String code) {
}
