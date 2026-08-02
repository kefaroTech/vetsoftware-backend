package com.vetsoftware.app.baserole.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBaseRoleRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 50) String code,
    @NotNull Boolean mandatory) {}
