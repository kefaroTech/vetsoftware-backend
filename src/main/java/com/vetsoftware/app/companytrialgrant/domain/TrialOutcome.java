package com.vetsoftware.app.companytrialgrant.domain;

/**
 * Cómo acabó una prueba. Vacío = prueba viva.
 *
 * <p>
 * Es la tasa de conversión por módulo con una sola consulta, y espeja
 * {@code chk_company_trial_grants_outcome}: o hay fecha de resolución y
 * desenlace, o no hay ninguno de los dos.
 */
public enum TrialOutcome {

    /** Pasó a pagar. */
    CONVERTED,

    /** Se quedó gratis con techo. */
    LIMITED,

    /** Quedó en consulta. */
    READ_ONLY,

    /**
     * La quitó antes de vencer. La concesión sigue existiendo: no se desconcede.
     */
    ABANDONED
}
