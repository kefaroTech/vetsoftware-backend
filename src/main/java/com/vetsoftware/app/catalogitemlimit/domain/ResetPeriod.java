package com.vetsoftware.app.catalogitemlimit.domain;

/**
 * Cada cuánto vuelve a cero un cupo de flujo.
 *
 * <p>
 * <strong>Vive aquí y no en el eje</strong>, y es una corrección del propio
 * diseño: cada cuánto se reinicia un cupo no es propiedad del eje sino de la
 * venta —el mismo eje se vende mensual a una clínica pequeña y semestral a una
 * grande—. Guardarlo en el eje hacía imposible justamente ese caso.
 */
public enum ResetPeriod {
    MONTH, QUARTER, SEMESTER
}
