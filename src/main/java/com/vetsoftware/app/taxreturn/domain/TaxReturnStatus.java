package com.vetsoftware.app.taxreturn.domain;

/**
 * En que punto esta la declaracion. Dominio cerrado y <strong>espejo
 * exacto</strong> de {@code chk_tax_returns_status}.
 *
 * <p>
 * <strong>Los cuatro valores son una decision propia y no del documento
 * maestro</strong> (§8.3 de la especificacion): sin estado, «presentada» y «en
 * borrador» son la misma fila y {@code firmeza_until} no se puede exigir — que
 * es la columna de la que cuelga toda la politica de conservacion de soportes.
 */
public enum TaxReturnStatus {

    /**
     * Borrador. Estado inicial y defecto de la columna. Aun no se presento nada.
     */
    DRAFT,

    /**
     * Presentada. Exige fecha, firmante, radicado, copia del fichero y
     * {@code firmezaUntil} posterior a la fecha de presentacion — las cinco cosas,
     * espejo de {@code chk_tax_returns_filed}.
     */
    FILED,

    /**
     * Corregida por una declaracion posterior del mismo periodo. Conserva sus datos
     * de presentacion —fue una declaracion real— pero <b>libera el hueco</b> de
     * {@code uq_tax_returns_current}.
     */
    CORRECTED,

    /**
     * Anulada. Vuelve a no tener datos de presentacion y tambien libera el hueco.
     */
    ANNULLED;

    /**
     * {@code true} si la declaracion es la vigente de su periodo, es decir si ocupa
     * el hueco de {@code uq_tax_returns_current}.
     *
     * <p>
     * <strong>Esta es la lista que calcula {@code current_return_marker} en la
     * base</strong> ({@code CASE WHEN status IN ('DRAFT','FILED') …}). Si las dos
     * dejaran de coincidir, Java creeria que el hueco esta libre y la base diria
     * que no —o al reves, que es peor: dos declaraciones vigentes del mismo
     * periodo, y nadie sabiendo cual vale.
     */
    public boolean occupiesTheCurrentSlot() {
        return this == DRAFT || this == FILED;
    }

    /** {@code true} si la declaracion lleva datos de presentacion. */
    public boolean isFiled() {
        return this == FILED || this == CORRECTED;
    }
}
