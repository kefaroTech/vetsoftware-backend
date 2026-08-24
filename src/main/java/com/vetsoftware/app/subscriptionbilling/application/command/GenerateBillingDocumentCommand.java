package com.vetsoftware.app.subscriptionbilling.application.command;

import com.vetsoftware.app.subscriptionbilling.domain.BillingReason;
import java.time.LocalDate;

/**
 * Calcular la cuenta de cobro de un contrato para un <b>periodo exacto</b>,
 * agrupando sus cargos pendientes.
 *
 * <p>
 * El periodo va como par de fechas y no como «mes» a propósito: la barandilla
 * contra la doble facturación agrupa por periodo exacto, de modo que la factura
 * anual emitida a mitad de agosto y la mensual del día 1 puedan coexistir.
 */
public record GenerateBillingDocumentCommand(Long companyId, Long subscriptionId,
        BillingReason billingReason, LocalDate periodStart, LocalDate periodEnd) {
}
