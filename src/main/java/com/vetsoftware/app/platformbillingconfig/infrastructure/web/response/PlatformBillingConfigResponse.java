package com.vetsoftware.app.platformbillingconfig.infrastructure.web.response;

import java.time.LocalDateTime;

/**
 * Las políticas de facturación tal como las ve la consola de plataforma.
 *
 * @param defaultPriceList
 *            tarifa por defecto; {@code null} si no hay ninguna configurada
 * @param defaultGraceDays
 *            días de cortesía tras el vencimiento antes de pasar la cuenta a
 *            solo lectura
 * @param invoiceDayOfMonth
 *            día del mes en que se emiten los cobros (1–28)
 * @param defaultPaymentTermDays
 *            días desde la emisión hasta el vencimiento; cero significa pago
 *            inmediato
 * @param externalBillingProvider
 *            sistema con el que se emiten las facturas de suscripción fuera de
 *            este software
 */
public record PlatformBillingConfigResponse(Long id, PriceListSummary defaultPriceList,
        int defaultGraceDays, int defaultTrialDays, int invoiceDayOfMonth,
        int defaultPaymentTermDays, String externalBillingProvider, LocalDateTime createdDate) {
}
