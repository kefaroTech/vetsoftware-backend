package com.vetsoftware.app.productchargeopenaccount.infrastructure.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProductChargeOpenAccountRequest(
        @NotNull(message = "Debes seleccionar la mascota.") Long animalId,
        @NotNull(message = "Debes seleccionar el producto.") Long productId,
        /**
         * Cantidad vendida; opcional (null = 1) para compatibilidad con clientes
         * previos.
         */
        @Min(value = 1, message = "La cantidad debe ser de al menos 1 unidad.") Integer quantity,
        @NotNull(message = "Debes seleccionar la cuenta abierta.") Long openAccountId,
        /**
         * Sede desde la que se vende (descuenta inventario). Opcional: null = sede
         * Principal (o única del empleado).
         */
        Long branchId,
        /**
         * Idempotency key opcional (UUID) para deduplicar reintentos del mismo cargo.
         */
        @Size(max = 36, message = "El identificador de la solicitud no puede superar los 36 caracteres.") String clientRequestId,
        /**
         * Versión optimista de la cuenta que vio el front (opt-in) para detección
         * temprana de conflicto.
         */
        Long expectedVersion) {
}
