package com.vetsoftware.app.subscriptionbilling.application.port.out;

import java.time.LocalDate;

/**
 * Mueve el periodo facturado del contrato al siguiente.
 *
 * <p>
 * <b>Este puerto es el que le faltaba al modelo, y su ausencia costaba dinero
 * de dos maneras.</b> {@code Subscription.renewPeriod} existía desde el primer
 * día y <b>no tenía un solo llamador en producción</b>: el periodo en curso de
 * un contrato no se movía nunca. Consecuencias:
 *
 * <ol>
 * <li>El barrido siguiente vuelve a mirar el mismo periodo y no encuentra nada
 * nuevo que cobrar: <b>el contrato deja de facturar después del primer mes</b>.
 * <li>El prorrateo de una conversión da <b>cero</b>. La fórmula mide los días
 * del cambio contra {@code current_period_start/end}, y si ese periodo se quedó
 * congelado en el pasado, el tramo afectado no lo cruza: cero días de
 * prorrateo, cero pesos, y ese cero se guardaba como si fuera un resultado.
 * </ol>
 *
 * <p>
 * <b>Cruza a {@code subscription} por su puerto de entrada, no por su
 * repositorio.</b> El avance es una mutación del agregado ajeno y tiene que
 * pasar por su dominio —{@code renewPeriod} valida que el periodo no nazca
 * invertido— y por su {@code @PreAuthorize}. Escribir sus columnas desde un
 * adaptador de esta rodaja habría saltado las dos cosas.
 */
public interface SubscriptionPeriodAdvancePort {

    void advanceTo(Long subscriptionId, Long companyId, LocalDate periodStart, LocalDate periodEnd,
            LocalDate nextBillingDate);
}
