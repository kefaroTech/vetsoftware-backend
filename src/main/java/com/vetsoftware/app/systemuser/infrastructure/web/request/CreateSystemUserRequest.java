package com.vetsoftware.app.systemuser.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSystemUserRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(min = 8, max = 100) String password
) {}
