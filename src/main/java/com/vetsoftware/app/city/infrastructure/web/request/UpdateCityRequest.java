package com.vetsoftware.app.city.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCityRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Long stateId
) {}
