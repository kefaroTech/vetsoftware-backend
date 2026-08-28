package com.vetsoftware.app.catalogitemlimit.domain;

/**
 * Cómo se mide el eje al que se le pone techo. Copia propia de esta rodaja: el
 * enum de {@code limitdimension} no se importa (vertical slicing), y el valor
 * está copiado también en la columna, atado por clave foránea contra
 * {@code limit_dimensions(id, measure_kind)} — cambiar el tipo de un eje con
 * artículos vendidos es un error del motor.
 */
public enum MeasureKind {
    STOCK, CUMULATIVE, FLOW;

    public boolean requiresResetPeriod() {
        return this == FLOW;
    }

    /**
     * El excedente no cabe sobre un acumulativo: cobrar por unidad sobre un
     * contador que no libera al borrar significa cobrar para siempre por registros
     * ya suprimidos.
     */
    public boolean admitsOverage() {
        return this != CUMULATIVE;
    }
}
