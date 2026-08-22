package com.vetsoftware.app.passwordreset.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "El token de recuperación es obligatorio.") String token,
        @NotBlank(message = "La nueva contraseña es obligatoria.") @Size(min = 8, max = 100, message = "La nueva contraseña debe tener entre 8 y 100 caracteres.") String newPassword) {
}
