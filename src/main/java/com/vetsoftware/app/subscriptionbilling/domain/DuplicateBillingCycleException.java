package com.vetsoftware.app.subscriptionbilling.domain;

import java.time.LocalDate;

/**
 * Ya hay una factura de ciclo viva para ese contrato y ese <b>periodo
 * exacto</b>. HTTP 409.
 *
 * <p>
 * Es la barandilla contra la doble facturacion, y agrupa por periodo exacto y
 * no por mes. Agrupando por mes, la factura anual emitida a mitad de agosto
 * chocaba con la mensual del dia 1 y <b>el cambio a plan anual era
 * irregistrable</b> -justo el cambio que mas caja trae-. Por periodo exacto
 * sigue impidiendo regenerar dos veces la factura del mismo periodo, que es la
 * doble facturacion real.
 *
 * <p>
 * Ultima linea de defensa en la base: {@code uq_sbd_recurring_cycle} sobre
 * {@code (recurring_cycle_marker, period_start, period_end)}.
 */
public class DuplicateBillingCycleException extends RuntimeException {
    public DuplicateBillingCycleException(Long subscriptionId, LocalDate periodStart,
            LocalDate periodEnd) {
        super("A recurring cycle invoice already exists for subscription " + subscriptionId
                + " and the exact period " + periodStart + " to " + periodEnd);
    }
}
