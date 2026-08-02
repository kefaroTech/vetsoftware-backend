package com.vetsoftware.app.role.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
    @NotBlank @Size(max = 100) String name, @NotBlank @Size(max = 50) String code) {}
