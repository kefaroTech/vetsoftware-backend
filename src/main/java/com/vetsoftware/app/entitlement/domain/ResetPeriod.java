package com.vetsoftware.app.entitlement.domain;

import java.time.LocalDate;

/**
 * Cada cuanto vuelve a empezar un cupo de flujo.
 *
 * <p>
 * <strong>Es propiedad de la venta, no del eje</strong>, y por eso no vive en
 * {@code limit_dimensions}: el mismo eje de citas se vende mensual a una
 * clinica pequeña y semestral a una grande. La granularidad baja al techo de
 * fabrica del articulo ({@code catalog_item_limits.reset_period}) y a su copia
 * congelada en el contrato ({@code subscription_item_limits.reset_period});
 * esta copia de rodaja espeja los dos {@code CHECK} que la acotan.
 *
 * <p>
 * Solo tiene sentido sobre un eje {@link MeasureKind#FLOW}. Los otros dos la
 * exigen nula --lo comprueba tambien el motor-- porque un cupo que no se mide
 * por periodo no tiene periodo que reiniciar.
 */
public enum ResetPeriod {

    /** {@code 2026-03}. */
    MONTH,

    /** {@code 2026-Q1}. */
    QUARTER,

    /** {@code 2026-S1}. */
    SEMESTER;

    /**
     * La clave del periodo al que pertenece ese dia con esta granularidad.
     *
     * <p>
     * Siempre siete caracteres, que es lo que exige {@link PeriodKey}: el texto
     * dice por si solo de que periodo habla, asi que una clave mensual y una
     * trimestral del mismo año no se pueden confundir al leerlas ni al indexarlas.
     */
    public String keyFor(LocalDate day) {
        if (day == null)
            throw new IllegalArgumentException("a period key needs the day it is computed from");
        int year = day.getYear();
        int month = day.getMonthValue();
        return switch (this) {
            case MONTH -> String.format("%04d-%02d", year, month);
            case QUARTER -> String.format("%04d-Q%d", year, (month - 1) / 3 + 1);
            case SEMESTER -> String.format("%04d-S%d", year, (month - 1) / 6 + 1);
        };
    }
}
