package com.vetsoftware.app.securityincident.domain;

/**
 * Gravedad del incidente. Espejo de {@code chk_security_incidents_severity}
 * (changeset 356).
 *
 * <p>
 * <strong>No decide el plazo.</strong> El de la Superintendencia de Industria y
 * Comercio son quince dias habiles para todos por igual —ver
 * {@link SecurityIncident#PLAZO_REPORTE_SIC_DIAS_HABILES}—; esta columna sirve
 * para priorizar la atencion interna, no para graduar la obligacion legal.
 */
public enum IncidentSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}
