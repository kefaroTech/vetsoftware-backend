package com.vetsoftware.app.quote.application.dto;

import java.time.LocalDate;

/**
 * @param firstChargeDate
 *            cuándo llega el primer cobro real de esta línea: el día siguiente
 *            al fin de la prueba si sucede a una {@code TRIAL} (sin prorrateo),
 *            o {@code subscriptions.next_billing_date} del contrato si es
 *            {@code NEVER_FREE} o una prueba ya vencida — el otrosí que abre
 *            esa línea devenga el cargo hoy pero no lo factura ni lo cobra
 *            hasta ese corte de ciclo.
 * @param chargedNow
 *            {@code firstChargeDate == hoy}. Casi siempre {@code false}: solo
 *            es {@code true} si el corte de ciclo del contrato cae hoy mismo.
 */
public record PurchasedModuleLineDto(String catalogItemCode, LocalDate firstChargeDate,
        boolean chargedNow) {
}
