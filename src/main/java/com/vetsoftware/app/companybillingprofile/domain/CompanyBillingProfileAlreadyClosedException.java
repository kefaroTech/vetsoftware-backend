package com.vetsoftware.app.companybillingprofile.domain;

import java.time.LocalDate;

/**
 * Se pidio cerrar una ficha que ya estaba cerrada.
 *
 * <p>
 * <strong>Es la capa de arriba de una defensa de tres.</strong> Debajo esta
 * {@code @Version}: si dos administradores de la misma empresa lanzan la
 * sucesion a la vez, los dos leen la misma ficha vigente, los dos pasan por
 * aqui, y el bloqueo optimista para al segundo en el {@code UPDATE}. Y por
 * debajo de los dos sigue {@code uq_company_billing_profiles_current}, que es
 * lo unico que el motor garantiza pase lo que pase en Java.
 */
public class CompanyBillingProfileAlreadyClosedException extends RuntimeException {

    public CompanyBillingProfileAlreadyClosedException(Long id, LocalDate validTo) {
        super("Company billing profile " + id + " was already closed on " + validTo);
    }
}
