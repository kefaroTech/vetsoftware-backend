package com.vetsoftware.app.subscriptionbilling.infrastructure.web.request;

import com.vetsoftware.app.subscriptionbilling.domain.BillingReason;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Calcular la cuenta de cobro de un contrato para un periodo exacto.
 *
 * <p>
 * El periodo se pide como par de fechas y no como mes: es lo que permite que la
 * factura anual emitida a mitad de agosto y la mensual del día 1 convivan.
 */
public record GenerateBillingDocumentRequest(@NotNull Long subscriptionId,
        @NotNull BillingReason billingReason, @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd) {
}
