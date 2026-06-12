package com.vetsoftware.app.economicactivity.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEconomicActivityRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 150) String name
) {}
