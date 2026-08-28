package com.vetsoftware.app.securityincident.domain;

/**
 * Una clinica alcanzada por un incidente, con el ambito por el que quedo
 * alcanzada y cuantos titulares suyos.
 *
 * <p>
 * <strong>Se escribe una sola vez, al cerrar el incidente</strong>, y no se
 * edita ni se borra nunca. Por eso la tabla no lleva {@code created_date}, ni
 * {@code enabled}, ni {@code version}: no hay una segunda escritura que pudiera
 * pisar a la primera.
 *
 * <p>
 * <strong>Quitar una clinica de esta lista es destruir la prueba de que se le
 * notifico.</strong> Ni el puerto de salida expone borrado, ni la entidad JPA
 * lleva {@code @SQLDelete}, ni el controller publica un {@code DELETE}. La
 * ausencia es la decision.
 *
 * <p>
 * El contador es <b>el de esa clinica</b>, no el del incidente entero: el total
 * declarado vive en {@link SecurityIncident#getAffectedSubjectCount()} y estas
 * filas son el reparto real.
 */
public class SecurityIncidentCompany {

    private final Long id;
    private final Long securityIncidentId;
    private final Long companyId;
    private final AffectedScope affectedScope;
    private final int affectedSubjectCount;

    public SecurityIncidentCompany(Long id, Long securityIncidentId, Long companyId,
            AffectedScope affectedScope, int affectedSubjectCount) {
        if (securityIncidentId == null)
            throw new IllegalArgumentException("securityIncidentId is required");
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (affectedScope == null)
            throw new IllegalArgumentException("affectedScope is required");
        if (affectedSubjectCount < 0)
            throw new IllegalArgumentException("affectedSubjectCount must not be negative");
        this.id = id;
        this.securityIncidentId = securityIncidentId;
        this.companyId = companyId;
        this.affectedScope = affectedScope;
        this.affectedSubjectCount = affectedSubjectCount;
    }

    /** Alta de la fila. Sin id: lo genera la base. */
    public static SecurityIncidentCompany register(Long securityIncidentId, Long companyId,
            AffectedScope affectedScope, int affectedSubjectCount) {
        return new SecurityIncidentCompany(null, securityIncidentId, companyId, affectedScope,
                affectedSubjectCount);
    }

    public Long getId() {
        return id;
    }

    public Long getSecurityIncidentId() {
        return securityIncidentId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public AffectedScope getAffectedScope() {
        return affectedScope;
    }

    public int getAffectedSubjectCount() {
        return affectedSubjectCount;
    }
}
