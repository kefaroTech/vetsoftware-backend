package com.vetsoftware.app.employee.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendInvitationRequest(
        @NotBlank(message = "La contraseña es obligatoria.") @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres.") String password) {
}
