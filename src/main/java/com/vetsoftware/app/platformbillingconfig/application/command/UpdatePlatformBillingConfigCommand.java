package com.vetsoftware.app.platformbillingconfig.application.command;

/**
 * Reemplaza en bloque las políticas de facturación de la plataforma.
 *
 * <p>
 * No lleva {@code id} ni {@code companyId} <b>a propósito</b>: la tabla tiene
 * una sola fila garantizada por el esquema, así que no hay nada que señalar, y
 * es configuración global de plataforma, así que no hay empresa a la que
 * pertenezca. El caso de uso está cerrado a {@code hasRole('SYSTEM')} a secas.
 *
 * @param defaultPriceListId
 *            tarifa por defecto, o {@code null} para dejar la plataforma sin
 *            tarifa por defecto
 * @param defaultPaymentTermDays
 *            días desde la emisión hasta el vencimiento; cero significa pago
 *            inmediato
 */
public record UpdatePlatformBillingConfigCommand(Long defaultPriceListId, Integer defaultGraceDays,
        Integer defaultTrialDays, Integer invoiceDayOfMonth, Integer defaultPaymentTermDays,
        String externalBillingProvider) {
}
