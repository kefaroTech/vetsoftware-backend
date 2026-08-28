package com.vetsoftware.app.companytrialwindow.domain;

/**
 * Ya hay una ventana abierta para esta empresa.
 *
 * <p>
 * La invariante la impone el índice único sobre la columna generada
 * {@code open_window_marker}; esta excepción es su traducción legible, y el
 * código la comprueba antes para que el operador lea qué pasó en vez de un
 * choque de clave. Lo que <strong>no</strong> hace el código es intentar
 * sortearla.
 */
public class CompanyAlreadyHasOpenTrialWindowException extends RuntimeException {

    public CompanyAlreadyHasOpenTrialWindowException(Long companyId) {
        super("Company " + companyId + " already has an open trial window:"
                + " a second one would make two ceilings valid at once");
    }
}
