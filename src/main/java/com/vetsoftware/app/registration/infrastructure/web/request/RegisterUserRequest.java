package com.vetsoftware.app.registration.infrastructure.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
    @NotBlank @Size(max = 100) String companyName,
    @NotBlank @Pattern(regexp = "\\d{5,15}",
            message = "El NIT debe ser numérico (entre 5 y 15 dígitos)") String companyIdentifier,
    @Size(max = 200) String companyAddress,
    @Size(max = 30) String companyContactNumber,
    @NotNull Long cityId,
    @NotBlank @Size(max = 100) String employeeName,
    @NotBlank @Email @Size(max = 100) String employeeEmail,
    @NotBlank @Size(min = 8, max = 100) String password,
    @NotBlank String taxRegime,
    @NotBlank @Email @Size(max = 255) String fiscalEmail
) {}
