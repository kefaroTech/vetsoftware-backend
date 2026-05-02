package com.vetsoftware.app.systemuser.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSystemUserRequest(
        @NotBlank @Size(max = 50) String code
) {}
