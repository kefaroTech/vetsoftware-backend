package com.vetsoftware.app.subscriptionbilling.application.dto;

import java.math.BigDecimal;

/**
 * El desenlace de emitir el documento de un periodo exacto.
 *
 * <p>
 * {@code issued} distingue el documento recién calculado del que ya existía: un
 * periodo duplicado o sin cargos pendientes devuelve {@code false} sin lanzar,
 * porque los dos son desenlaces normales de la operación (ver
 * {@code IssueSubscriptionPeriodDocumentUseCase}).
 *
 * <p>
 * {@code currency} viaja fija en {@code "COP"}: el documento de cobro no modela
 * todavía la divisa.
 */
public record IssuedPeriodDocumentDto(Long documentId, String documentNumber,
        BigDecimal totalAmount, String currency, boolean issued, int accruedCharges) {
}
