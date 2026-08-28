package com.vetsoftware.app.companytrialwindow.domain;

import java.time.LocalDateTime;

/**
 * Cerrar dos veces la misma ventana. No es un no-op inofensivo: la segunda
 * llamada movería la fecha en que la empresa dejó de estar en prueba, que es
 * justo el dato con el que se audita quién probó qué y hasta cuándo.
 */
public class TrialWindowAlreadyClosedException extends RuntimeException {

    public TrialWindowAlreadyClosedException(Long companyId, LocalDateTime closedAt) {
        super("Trial window of company " + companyId + " was already closed at " + closedAt);
    }
}
