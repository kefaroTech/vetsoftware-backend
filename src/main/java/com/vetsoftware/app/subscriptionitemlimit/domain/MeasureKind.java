package com.vetsoftware.app.subscriptionitemlimit.domain;

/**
 * Cómo se mide el eje. Copia propia de esta rodaja, y copiada además en la
 * columna, atada por clave foránea contra {@code limit_dimensions(id,
 * measure_kind)}.
 */
public enum MeasureKind {
    STOCK, CUMULATIVE, FLOW;

    public boolean requiresResetPeriod() {
        return this == FLOW;
    }

    public boolean admitsOverage() {
        return this != CUMULATIVE;
    }
}
