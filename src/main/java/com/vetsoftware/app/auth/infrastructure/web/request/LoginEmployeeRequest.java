package com.vetsoftware.app.auth.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record LoginEmployeeRequest(@NotBlank String employeeCode, @NotBlank String password) {
}
