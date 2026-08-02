package com.vetsoftware.app.coderecovery.infrastructure.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecoverCodeRequest(@NotBlank @Email @Size(max = 100) String email) {}
