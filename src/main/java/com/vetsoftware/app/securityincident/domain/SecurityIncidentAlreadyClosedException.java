package com.vetsoftware.app.securityincident.domain;

import java.time.LocalDateTime;

/**
 * Un incidente cerrado no se vuelve a cerrar: la contencion y la causa raiz
 * escritas en su momento son el expediente, y reescribirlas mas tarde con otra
 * narracion es indistinguible de haberlo redactado despues de los hechos.
 */
public class SecurityIncidentAlreadyClosedException extends RuntimeException {

    public SecurityIncidentAlreadyClosedException(Long id, LocalDateTime closedAt) {
        super("Security incident " + id + " was already closed at " + closedAt
                + ": containment and root cause are the record and are not rewritten");
    }
}
