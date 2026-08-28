package com.vetsoftware.app.securityincident.domain;

import java.time.LocalDateTime;

/**
 * Reportar dos veces el mismo incidente machacaria la fecha que prueba que se
 * aviso dentro del plazo. Es un conflicto de estado, no un dato mal escrito: la
 * peticion esta bien formada y choca con lo que ya consta.
 */
public class SecurityIncidentAlreadyReportedException extends RuntimeException {

    public SecurityIncidentAlreadyReportedException(Long id, LocalDateTime reportedAt) {
        super("Security incident " + id + " was already reported to the authority at " + reportedAt
                + ": the report date proves the deadline was met and is not overwritten");
    }
}
