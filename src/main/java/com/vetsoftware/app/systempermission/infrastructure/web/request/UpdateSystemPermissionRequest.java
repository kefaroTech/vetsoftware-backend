package com.vetsoftware.app.systempermission.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSystemPermissionRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 50) String code
) {}
