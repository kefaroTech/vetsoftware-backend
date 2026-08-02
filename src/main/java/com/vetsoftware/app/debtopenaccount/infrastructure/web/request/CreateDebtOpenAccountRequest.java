package com.vetsoftware.app.debtopenaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateDebtOpenAccountRequest(
    @NotNull @Positive BigDecimal amount,
    @NotBlank String paymentMethod,
    @NotNull Long openAccountId,
    /** Idempotency key opcional (UUID) para deduplicar reintentos del mismo cobro. */
    @Size(max = 36) String clientRequestId,
    /**
     * Versión optimista de la cuenta que vio el front (opt-in) para detección temprana de
     * conflicto.
     */
    Long expectedVersion) {}
