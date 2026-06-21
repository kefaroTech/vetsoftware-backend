package com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record VoidServiceChargeOpenAccountRequest(
        @NotBlank String reason,
        /** Versión optimista de la cuenta que vio el front (opt-in) para detección temprana de conflicto. */
        Long expectedVersion) {}
