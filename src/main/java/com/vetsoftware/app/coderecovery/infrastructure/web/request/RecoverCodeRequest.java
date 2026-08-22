package com.vetsoftware.app.coderecovery.infrastructure.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecoverCodeRequest(
        @NotBlank(message = "El correo electrónico es obligatorio.") @Email(message = "El correo electrónico no tiene un formato válido.") @Size(max = 100, message = "El correo electrónico no puede superar los 100 caracteres.") String email) {
}
