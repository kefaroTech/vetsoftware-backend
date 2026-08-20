package com.vetsoftware.app.debtopenaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo del DELETE de un abono. El motivo es obligatorio porque la baja mueve
 * dinero (sube el saldo pendiente y compensa la caja) y queda registrada como
 * anulacion.
 */
public record DeleteDebtOpenAccountRequest(@NotBlank String reason,
        /**
         * Versión optimista de la cuenta que vio el front (opt-in) para detección
         * temprana de conflicto.
         */
        Long expectedVersion) {
}
