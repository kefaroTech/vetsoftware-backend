package com.vetsoftware.app.subscriptionbilling.application.command;

import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Devengar un cargo: el servicio se prestó, con o sin factura todavía.
 *
 * <p>
 * <b>No hay {@code taxAmount} y no es un olvido.</b> El cargo guarda su
 * {@code taxRate} y su {@code taxTreatment} —su base— y el importe del impuesto
 * se calcula una sola vez sobre la base agregada del documento. Añadirlo aquí
 * reabre un bloqueante cerrado.
 *
 * <p>
 * {@code prorationDays} y {@code periodDays} van juntos o no van: sin los dos,
 * el prorrateo se ve pero no se puede reconstruir.
 */
public record CreateSubscriptionChargeCommand(Long companyId, Long subscriptionId,
        Long subscriptionItemId, ChargeType chargeType, String description,
        LocalDate servicePeriodStart, LocalDate servicePeriodEnd, BigDecimal quantity,
        BigDecimal unitAmount, BigDecimal subtotalAmount, BigDecimal taxRate,
        TaxTreatment taxTreatment, Integer prorationDays, Integer periodDays, Long amendmentId) {
}
