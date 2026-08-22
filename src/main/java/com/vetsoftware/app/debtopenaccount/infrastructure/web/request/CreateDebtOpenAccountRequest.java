package com.vetsoftware.app.debtopenaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateDebtOpenAccountRequest(
        @NotNull(message = "El monto es obligatorio.") @Positive(message = "El monto debe ser mayor que cero.") BigDecimal amount,
        @NotBlank(message = "Debes seleccionar el método de pago.") String paymentMethod,
        @NotNull(message = "Debes seleccionar la cuenta abierta.") Long openAccountId,
        /**
         * Idempotency key opcional (UUID) para deduplicar reintentos del mismo cobro.
         */
        @Size(max = 36, message = "El identificador de la solicitud no puede superar los 36 caracteres.") String clientRequestId,
        /**
         * Versión optimista de la cuenta que vio el front (opt-in) para detección
         * temprana de conflicto.
         */
        Long expectedVersion) {
}
