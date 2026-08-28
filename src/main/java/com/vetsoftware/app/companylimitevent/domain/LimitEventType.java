package com.vetsoftware.app.companylimitevent.domain;

/**
 * Qué pasó con el cupo. Espeja {@code chk_company_limit_events_type}.
 *
 * <p>
 * Cinco de los seis nacen cuando alguien intenta algo.
 * {@link #OVER_LIMIT_ON_DOWNGRADE} no: el momento en que un cliente pasa a
 * estar por encima del techo no lo intenta nadie, <em>le pasa</em>, y lo
 * escribe el recálculo.
 */
public enum LimitEventType {

    /** Se le avisó: cruzó el porcentaje de aviso. */
    THRESHOLD_WARNED,

    /** Se le negó crear. Es la fila que hoy no se escribe. */
    LIMIT_BLOCKED,

    /** Se le subió el techo. */
    LIMIT_RAISED,

    /** El recuento periódico corrigió una desviación del contador. */
    USAGE_RECONCILED,

    /**
     * Una persona de plataforma corrigió el consumo, con motivo obligatorio. Es la
     * válvula de escape de D-12.
     */
    USAGE_ADJUSTED,

    /** El techo bajó por debajo de lo que el cliente ya tenía. */
    OVER_LIMIT_ON_DOWNGRADE;

    /** Solo la corrección manual exige motivo escrito. */
    public boolean requiresReason() {
        return this == USAGE_ADJUSTED;
    }
}
