package com.vetsoftware.app.registration.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(
        @NotBlank(message = "El token de verificación es obligatorio.") @Size(max = 200, message = "El token de verificación no puede superar los 200 caracteres.") String token) {
}
