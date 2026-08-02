package com.vetsoftware.app.country.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCountryRequest(@NotBlank @Size(max = 100) String name) {}
