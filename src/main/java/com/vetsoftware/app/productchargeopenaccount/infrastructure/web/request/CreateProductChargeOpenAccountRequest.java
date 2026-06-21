package com.vetsoftware.app.productchargeopenaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProductChargeOpenAccountRequest(
        @NotNull Long animalId,
        @NotNull Long productId,
        @NotNull Long openAccountId,
        /** Idempotency key opcional (UUID) para deduplicar reintentos del mismo cargo. */
        @Size(max = 36) String clientRequestId,
        /** Versión optimista de la cuenta que vio el front (opt-in) para detección temprana de conflicto. */
        Long expectedVersion
) {}
