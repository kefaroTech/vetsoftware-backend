package com.vetsoftware.app.productchargeopenaccount.infrastructure.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProductChargeOpenAccountRequest(
        @NotNull Long animalId,
        @NotNull Long productId,
        /** Cantidad vendida; opcional (null = 1) para compatibilidad con clientes previos. */
        @Min(1) Integer quantity,
        @NotNull Long openAccountId,
        /** Sede desde la que se vende (descuenta inventario). Opcional: null = sede Principal (o única del empleado). */
        Long branchId,
        /** Idempotency key opcional (UUID) para deduplicar reintentos del mismo cargo. */
        @Size(max = 36) String clientRequestId,
        /** Versión optimista de la cuenta que vio el front (opt-in) para detección temprana de conflicto. */
        Long expectedVersion
) {}
