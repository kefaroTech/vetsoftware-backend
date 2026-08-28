package com.vetsoftware.app.securityincident.application.command;

import java.time.LocalDateTime;

/**
 * Anota que el incidente se reporto a la Superintendencia de Industria y
 * Comercio.
 *
 * <p>
 * {@code reportedAt} viene por el cuerpo y no lo pone el reloj: el reporte se
 * hace por el micrositio de la Delegatura, fuera de este sistema, y la fecha
 * que vale es la de alli. Sellarla con el reloj del servidor convertiria «se
 * reporto el dia 12» en «se registro el dia 19», que es la diferencia entre
 * cumplir y no.
 *
 * @param reportReference
 *            el radicado. Obligatorio junto con la fecha —espejo de
 *            {@code chk_security_incidents_report}—: un reporte que no se puede
 *            rastrear no consta
 */
public record ReportSecurityIncidentCommand(Long id, LocalDateTime reportedAt,
        String reportReference) {
}
