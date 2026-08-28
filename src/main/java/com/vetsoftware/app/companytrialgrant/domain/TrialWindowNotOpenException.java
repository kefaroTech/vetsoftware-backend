package com.vetsoftware.app.companytrialgrant.domain;

import java.time.LocalDate;

/**
 * Se intentó conceder una prueba fuera de la ventana.
 *
 * <p>
 * R-TRIAL-09: la ventana estaba abierta cuando se concedió la prueba. El cierre
 * vive en otra fila y una restricción del motor no puede mirarla, así que esta
 * comprobación es código —y por eso tiene que estar escrita, no supuesta—.
 * Añadir un módulo el día 35 de una ventana de 30 entra pagando, no en prueba.
 */
public class TrialWindowNotOpenException extends RuntimeException {

    public TrialWindowNotOpenException(Long companyId, LocalDate day, LocalDate windowEndDate) {
        super("Company " + companyId + " has no open trial window on " + day + " (window ended "
                + windowEndDate + "): the module is added as paid, not as a trial");
    }

    /** Cuando no hay ninguna ventana viva de la que hablar. */
    public TrialWindowNotOpenException(Long companyId, LocalDate day) {
        super("Company " + companyId + " has no open trial window on " + day
                + ": the module is added as paid, not as a trial");
    }
}
