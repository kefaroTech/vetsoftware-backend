package com.vetsoftware.app.debtopenaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record VoidDebtOpenAccountRequest(@NotBlank String reason) {}
