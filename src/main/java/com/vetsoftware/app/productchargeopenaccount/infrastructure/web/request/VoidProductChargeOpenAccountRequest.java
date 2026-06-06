package com.vetsoftware.app.productchargeopenaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record VoidProductChargeOpenAccountRequest(@NotBlank String reason) {}
