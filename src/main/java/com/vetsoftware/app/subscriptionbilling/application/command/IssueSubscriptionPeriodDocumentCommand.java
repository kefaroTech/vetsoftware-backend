package com.vetsoftware.app.subscriptionbilling.application.command;

import java.time.LocalDate;

/**
 * Emitir la cuenta de cobro de UN contrato para UN <b>periodo exacto</b>, sin
 * mover su calendario de facturación.
 *
 * <p>
 * El periodo va como par de fechas y no como «mes», por la misma razón que
 * {@link GenerateBillingDocumentCommand}: la barandilla contra la doble
 * facturación agrupa por periodo exacto.
 */
public record IssueSubscriptionPeriodDocumentCommand(Long companyId, Long subscriptionId,
        LocalDate periodStart, LocalDate periodEnd) {
}
