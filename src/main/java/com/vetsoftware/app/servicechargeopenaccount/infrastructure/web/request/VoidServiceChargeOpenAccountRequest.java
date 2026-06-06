package com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record VoidServiceChargeOpenAccountRequest(@NotBlank String reason) {}
