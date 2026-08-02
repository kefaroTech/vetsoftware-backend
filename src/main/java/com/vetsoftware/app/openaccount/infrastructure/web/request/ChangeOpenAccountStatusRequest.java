package com.vetsoftware.app.openaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeOpenAccountStatusRequest(
    @NotBlank String status,
    // Opcional a nivel HTTP: la obligatoriedad del motivo en CANCEL la enforza el dominio.
    String reason,
    // Solo se usan al cerrar (CLOSE): tipo de documento a auto-emitir ("DOC_EQUIV_POS" |
    // "FE_VENTA";
    // null → DOC_EQUIV_POS) y si es venta a consumidor final. Se ignoran en CANCEL.
    String documentType,
    boolean finalConsumer,
    // Versión optimista de la cuenta que vio el front (opt-in) para detección temprana de
    // conflicto.
    Long expectedVersion) {}
