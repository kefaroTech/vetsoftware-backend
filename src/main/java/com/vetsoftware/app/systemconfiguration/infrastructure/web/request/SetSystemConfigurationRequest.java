package com.vetsoftware.app.systemconfiguration.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetSystemConfigurationRequest(
    @NotBlank @Size(max = 100) String propertyName, @NotBlank @Size(max = 255) String value) {}
