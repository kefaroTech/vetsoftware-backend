package com.vetsoftware.app.employee.infrastructure.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateEmployeeRequest(
        @NotBlank @Size(max = 50) String employeeCode,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 100) String email,
        @NotNull Long companyId,
        // Roles a asignar en el alta (al menos uno). Se asignan y se incluye el rol en la invitación.
        @NotEmpty List<Long> roleIds
) {}
