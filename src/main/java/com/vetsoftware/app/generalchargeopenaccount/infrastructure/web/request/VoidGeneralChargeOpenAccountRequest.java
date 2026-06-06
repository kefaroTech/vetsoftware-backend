package com.vetsoftware.app.generalchargeopenaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record VoidGeneralChargeOpenAccountRequest(@NotBlank String reason) {}
