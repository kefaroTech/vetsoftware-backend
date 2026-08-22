package com.vetsoftware.app.membership.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMembershipRequest(
        @NotBlank(message = "El nombre de la membresía es obligatorio.") @Size(max = 100, message = "El nombre de la membresía no puede superar los 100 caracteres.") String name,
        @NotBlank(message = "Debes seleccionar el estado de la membresía.") String status,
        boolean mandatory) {
}
