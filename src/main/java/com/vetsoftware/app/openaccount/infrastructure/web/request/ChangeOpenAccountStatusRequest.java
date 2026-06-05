package com.vetsoftware.app.openaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeOpenAccountStatusRequest(
        @NotBlank String status
) {}
