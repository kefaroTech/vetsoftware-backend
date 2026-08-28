package com.vetsoftware.app.securityincident.application.command;

import java.time.LocalDateTime;

/**
 * Cierra el incidente.
 *
 * <p>
 * Espejo de {@code chk_security_incidents_close}: {@code containment} y
 * {@code rootCause} son obligatorios. Un incidente que no se documento en su
 * momento es indistinguible de uno que se oculto.
 *
 * @param notifiedSubjectsAt
 *            cuando se informo a los titulares, si se hizo.
 *            <strong>Opcional</strong>: en Colombia la obligacion es informar a
 *            la autoridad, no a los titulares, y por eso esta columna no tiene
 *            plazo asociado. Se escribe al cerrar porque el cierre es cuando el
 *            expediente se completa
 */
public record CloseSecurityIncidentCommand(Long id, LocalDateTime closedAt, String containment,
        String rootCause, LocalDateTime notifiedSubjectsAt) {
}
