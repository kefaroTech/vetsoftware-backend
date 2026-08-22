package com.vetsoftware.app.systemuser.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSystemUserRequest(
        @NotBlank(message = "El código de usuario es obligatorio.") @Size(max = 50, message = "El código de usuario no puede superar los 50 caracteres.") String code,
        @NotBlank(message = "La contraseña es obligatoria.") @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres.") String password) {
}
