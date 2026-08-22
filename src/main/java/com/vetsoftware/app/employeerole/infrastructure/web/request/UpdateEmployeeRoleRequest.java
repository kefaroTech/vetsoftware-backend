package com.vetsoftware.app.employeerole.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record UpdateEmployeeRoleRequest(
        @NotNull(message = "Debes seleccionar el empleado.") Long employeeId,
        @NotNull(message = "Debes seleccionar el rol.") Long roleId) {
}
