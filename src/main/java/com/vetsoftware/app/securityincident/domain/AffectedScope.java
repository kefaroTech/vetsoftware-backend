package com.vetsoftware.app.securityincident.domain;

/**
 * Que quedo alcanzado en una clinica concreta. Espejo de {@code chk_sic_scope}
 * (changeset 357).
 *
 * <p>
 * <strong>El ambito entra en la unicidad de la puente</strong>
 * ({@code uq_sic_pair}, sobre {@code (security_incident_id, company_id,
 * affected_scope)}) y por eso es un dato y no una nota: un ataque que expone
 * credenciales <em>y</em> datos clinicos de la misma clinica son dos hechos
 * distintos con dos alcances distintos, y con una clave sin el ambito el
 * segundo seria inescribible.
 */
public enum AffectedScope {
    /** Datos personales de los titulares. */
    PERSONAL_DATA,
    /** Historia clinica de los pacientes. */
    CLINICAL_DATA,
    /** Documentos y datos de cobro. */
    BILLING_DATA,
    /** Credenciales de acceso. */
    CREDENTIALS
}
