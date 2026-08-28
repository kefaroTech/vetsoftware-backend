package com.vetsoftware.app.entitlement.domain;

/**
 * Como se mide el eje al que el contador le pone techo. Copia propia de esta
 * rodaja: el enum de {@code limitdimension} no se importa (vertical slicing), y
 * el valor esta copiado tambien en la columna {@code measure_kind} de
 * {@code company_capacities}, atada por clave foranea contra
 * {@code limit_dimensions(id, measure_kind)}.
 *
 * <p>
 * Esa atadura es R-LIMIT-22 y no es decorativa: sin ella, cambiar un eje de
 * {@code CUMULATIVE} a {@code FLOW} con contadores colgando dejaria un cupo
 * mensual que no se reinicia nunca, en silencio. Con ella es un error del
 * motor.
 */
public enum MeasureKind {

    /** Libera la plaza al instante: dar de baja un empleado devuelve su hueco. */
    STOCK,

    /**
     * No libera al borrar --o libera tras un enfriamiento--: cuenta lo registrado
     * historicamente.
     */
    CUMULATIVE,

    /**
     * Se mide por periodo. No hay proceso que ponga contadores a cero: al entrar el
     * periodo siguiente nace una fila nueva con su propia clave (R-LIMIT-04).
     */
    FLOW;

    /**
     * Solo el flujo tiene periodo real. Los demas llevan el centinela de
     * {@link PeriodKey}, nunca vacio (R-LIMIT-05).
     */
    public boolean requiresPeriodKey() {
        return this == FLOW;
    }
}
