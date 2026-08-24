package com.vetsoftware.app.subscriptionbilling.application.port.out;

/**
 * La FK compuesta {@code fk_subscription_charges_amendment} sobre
 * {@code (company_id, amendment_id)}, comprobada antes de construir el cargo.
 *
 * <p>
 * Mismo regimen que {@link SubscriptionItemValidationPort} y por las mismas
 * razones: el cargo guarda de que otrosi nacio, pero no lee ningun campo suyo,
 * asi que un {@code ValidationPort} acotado por empresa es toda la dependencia
 * que hace falta.
 *
 * <p>
 * <b>El escenario que lo justifica.</b> Durante el cierre mensual un operador
 * de plataforma devenga un cargo para la Clinica San Roque copiando el
 * {@code amendmentId} de un otrosi que en realidad es de la Clinica Los Andes
 * —dos pestanas abiertas, ids consecutivos—. Sin esta comprobacion recibe un
 * 500 de {@code fk_subscription_charges_amendment} y tiene que ir al log de la
 * base para saber cual de los ids del cuerpo estaba mal.
 */
public interface SubscriptionAmendmentValidationPort {

    /**
     * {@code true} si el otrosi existe <b>y es de esa empresa</b>.
     *
     * <p>
     * {@code subscription_amendments} no tiene {@code enabled} a proposito —un
     * otrosi no se retira, se emite otro—, asi que aqui no hay nada que filtrar mas
     * alla del par {@code (company_id, id)} que la FK compuesta exige.
     */
    boolean existsInCompany(Long amendmentId, Long companyId);
}
