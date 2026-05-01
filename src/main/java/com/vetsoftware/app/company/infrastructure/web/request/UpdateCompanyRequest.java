package com.vetsoftware.app.company.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 50) String identifier,
        @Size(max = 255) String address,
        @Size(max = 30) String contactNumber
) {}
