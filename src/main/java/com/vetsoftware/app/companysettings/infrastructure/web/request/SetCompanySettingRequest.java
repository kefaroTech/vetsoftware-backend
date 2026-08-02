package com.vetsoftware.app.companysettings.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetCompanySettingRequest(
    @NotBlank @Size(max = 100) String propertyName, @NotBlank @Size(max = 255) String value) {}
