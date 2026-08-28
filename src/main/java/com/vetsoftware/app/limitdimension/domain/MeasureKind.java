package com.vetsoftware.app.limitdimension.domain;

/**
 * Cómo se mide un eje limitable. Son tres y no dos, y confundirlas es el error
 * clásico: hace que un cupo mensual no se reinicie nunca, o que un cupo total
 * se borre cada mes.
 *
 * <p>
 * Espeja {@code chk_limit_dimensions_measure_kind}. El valor se copia dentro de
 * {@code catalog_item_limits}, {@code subscription_item_limits} y
 * {@code company_capacities}, y esa copia va atada por clave foránea contra
 * {@code uq_limit_dimensions_id_measure_kind}: cambiar el tipo de un eje ya
 * vendido es un error del motor, no un informe raro.
 */
public enum MeasureKind {

    /** Cuántas hay ahora mismo; dar de baja libera la plaza en el acto. */
    STOCK,

    /**
     * Cuántas ha habido, con enfriamiento tras el borrado (D-61). Es el único tipo
     * que exige {@code releaseDelayDays}, y el único sobre el que el excedente está
     * prohibido: cobrar por unidad sobre un contador que no libera al borrar
     * significa cobrar para siempre por registros ya suprimidos.
     */
    CUMULATIVE,

    /**
     * Cuántas por periodo. El contador vuelve a cero solo, naciendo una fila nueva
     * al entrar el periodo siguiente: ningún proceso pone contadores a cero.
     */
    FLOW;

    /** Los días de enfriamiento solo existen —y son obligatorios— aquí. */
    public boolean requiresReleaseDelay() {
        return this == CUMULATIVE;
    }

    /** Solo los de flujo declaran cada cuánto vuelven a cero. */
    public boolean requiresResetPeriod() {
        return this == FLOW;
    }

    /** El excedente no cabe sobre un acumulativo. */
    public boolean admitsOverage() {
        return this != CUMULATIVE;
    }
}
