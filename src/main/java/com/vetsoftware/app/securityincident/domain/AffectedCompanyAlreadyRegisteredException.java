package com.vetsoftware.app.securityincident.domain;

/**
 * Espejo de {@code uq_sic_pair (security_incident_id, company_id,
 * affected_scope)}.
 *
 * <p>
 * <strong>El ambito entra en la clave</strong>: la misma clinica puede constar
 * dos veces en el mismo incidente si quedo alcanzada por dos cosas distintas
 * —credenciales y datos clinicos—. Lo que no cabe es la misma terna dos veces,
 * que duplicaria el contador de titulares.
 */
public class AffectedCompanyAlreadyRegisteredException extends RuntimeException {

    public AffectedCompanyAlreadyRegisteredException(Long incidentId, Long companyId,
            AffectedScope scope) {
        super("Company " + companyId + " is already registered as affected by security incident "
                + incidentId + " with scope " + scope
                + ": registering it twice would double the subject count");
    }
}
