package com.vetsoftware.app.generalchargeopenaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateGeneralChargeOpenAccountRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull @PositiveOrZero BigDecimal unitAmount,
        @NotNull @Positive BigDecimal quantity,
        Long taxId,
        @NotNull Long openAccountId,
        /** Idempotency key opcional (UUID) para deduplicar reintentos del mismo cargo. */
        @Size(max = 36) String clientRequestId,
        /** Versión optimista de la cuenta que vio el front (opt-in) para detección temprana de conflicto. */
        Long expectedVersion
) {}
