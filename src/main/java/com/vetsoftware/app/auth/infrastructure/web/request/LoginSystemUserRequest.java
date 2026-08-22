package com.vetsoftware.app.auth.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record LoginSystemUserRequest(
        @NotBlank(message = "El código de usuario es obligatorio.") String code,
        @NotBlank(message = "La contraseña es obligatoria.") String password) {
}
