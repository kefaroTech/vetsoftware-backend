package com.vetsoftware.app.employee.infrastructure.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEmployeeRequest(
        @NotBlank(message = "El código del empleado es obligatorio.") @Size(max = 50, message = "El código del empleado no puede superar los 50 caracteres.") String employeeCode,
        @NotBlank(message = "El nombre del empleado es obligatorio.") @Size(max = 100, message = "El nombre del empleado no puede superar los 100 caracteres.") String name,
        @NotBlank(message = "El correo electrónico es obligatorio.") @Email(message = "El correo electrónico no tiene un formato válido.") @Size(max = 100, message = "El correo electrónico no puede superar los 100 caracteres.") String email) {
}
