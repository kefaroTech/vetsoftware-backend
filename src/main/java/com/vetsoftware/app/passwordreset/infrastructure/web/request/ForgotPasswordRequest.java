package com.vetsoftware.app.passwordreset.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank(message = "El código del empleado es obligatorio.") @Size(max = 50, message = "El código del empleado no puede superar los 50 caracteres.") String employeeCode) {
}
