package com.vetsoftware.app.generalchargeopenaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record VoidGeneralChargeOpenAccountRequest(
        @NotBlank(message = "El motivo de la anulación es obligatorio.") String reason,
        /**
         * Versión optimista de la cuenta que vio el front (opt-in) para detección
         * temprana de conflicto.
         */
        Long expectedVersion) {
}
