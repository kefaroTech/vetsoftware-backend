package com.vetsoftware.app.spatype.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSpaTypeRequest(
    @NotBlank @Size(max = 100) String name, @Size(max = 500) String description) {}
