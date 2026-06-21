package com.vetsoftware.app.productchargeopenaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record UpdateProductChargeOpenAccountRequest(
        @NotNull Long animalId,
        @NotNull Long productId,
        @NotNull Long openAccountId,
        /** Versión optimista de la cuenta que vio el front (opt-in) para detección temprana de conflicto. */
        Long expectedVersion
) {}
