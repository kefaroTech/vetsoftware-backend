package com.vetsoftware.app.employee.infrastructure.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEmployeeRequest(@NotBlank @Size(max = 50) String employeeCode,
        @NotBlank @Size(max = 100) String name, @NotBlank @Email @Size(max = 100) String email) {
}
