package com.vetsoftware.app.employee.infrastructure.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateEmployeeRequest(
        @NotBlank(message = "El código del empleado es obligatorio.") @Size(max = 50, message = "El código del empleado no puede superar los 50 caracteres.") String employeeCode,
        // Ojo al orden de los dos mensajes: una contraseña vacía viola @NotBlank Y el
        // min de @Size a la vez, y Hibernate Validator no garantiza cuál entrega
        // primero. GlobalExceptionHandler.fieldErrors los agrupa en una sola entrada
        // de `password`, así que los dos textos tienen que poder leerse seguidos.
        @NotBlank(message = "La contraseña es obligatoria.") @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres.") String password,
        @NotBlank(message = "El nombre del empleado es obligatorio.") @Size(max = 100, message = "El nombre del empleado no puede superar los 100 caracteres.") String name,
        @NotBlank(message = "El correo electrónico es obligatorio.") @Email(message = "El correo electrónico no tiene un formato válido.") @Size(max = 100, message = "El correo electrónico no puede superar los 100 caracteres.") String email,
        // Roles a asignar en el alta (al menos uno). Se asignan y se incluye el rol en
        // la invitación.
        @NotEmpty(message = "Debes asignar al menos un rol.") List<Long> roleIds,
        // Sedes a asignar en el alta (al menos una). Un empleado no puede crearse sin
        // sede.
        @NotEmpty(message = "Debes asignar al menos una sede.") List<Long> branchIds) {
}
