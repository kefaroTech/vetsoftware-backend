package com.vetsoftware.app.auth.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record LoginSystemUserRequest(@NotBlank String code, @NotBlank String password) {}
