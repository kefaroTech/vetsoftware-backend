package com.vetsoftware.app.submodule.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSubModuleRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 50) String code,
    @NotNull Long moduleId) {}
