package com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateServiceChargeOpenAccountRequest(
        @NotNull(message = "Debes seleccionar la mascota.") Long animalId,
        @NotNull(message = "Debes seleccionar el servicio.") Long serviceId,
        @NotNull(message = "Debes seleccionar la cuenta abierta.") Long openAccountId,
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
