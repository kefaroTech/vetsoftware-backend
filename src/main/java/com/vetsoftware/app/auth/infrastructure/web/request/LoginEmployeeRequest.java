package com.vetsoftware.app.auth.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record LoginEmployeeRequest(
        @NotBlank(message = "El código del empleado es obligatorio.") String employeeCode,
        @NotBlank(message = "La contraseña es obligatoria.") String password) {
}
