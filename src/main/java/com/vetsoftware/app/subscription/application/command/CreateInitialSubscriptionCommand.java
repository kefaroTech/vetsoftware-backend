package com.vetsoftware.app.subscription.application.command;

import com.vetsoftware.app.subscription.domain.BillingCycle;
import java.time.LocalDate;

/**
 * Alta del contrato con el que nace una empresa. No trae ni tarifa, ni precio,
 * ni articulo: todo eso sale del minimo estructural de la plataforma, que es lo
 * unico legitimo de lo que puede colgar un contrato que nadie negocio.
 *
 * @param companyId
 *            la empresa recien creada, en la misma transaccion del alta
 * @param billingCycle
 *            el ciclo con el que arranca; {@code null} vale MONTHLY
 * @param startDate
 *            el dia en que arranca; {@code null} vale hoy
 * @param trialEndDate
 *            fin de la ventana de prueba de la empresa, o {@code null} si este
 *            alta no abre ventana. Con un valor no nulo, el contrato firma una
 *            linea {@code TRIAL} por cada articulo {@code ELIGIBLE} en vez del
 *            nucleo mas las capacidades del minimo estructural.
 */
public record CreateInitialSubscriptionCommand(Long companyId, BillingCycle billingCycle,
        LocalDate startDate, LocalDate trialEndDate) {

    /** Alta sin ventana de prueba: ninguna línea se firma como {@code TRIAL}. */
    public CreateInitialSubscriptionCommand(Long companyId, BillingCycle billingCycle,
            LocalDate startDate) {
        this(companyId, billingCycle, startDate, null);
    }
}
