package com.vetsoftware.app.paymentreversal.domain;

/**
 * Como termino el expediente. Espejo de {@code chk_prr_outcome}.
 *
 * <p>
 * {@link #ACCEPTED} y {@link #PARTIALLY_ACCEPTED} mueven dinero y por eso
 * exigen importe aplicado; {@link #REJECTED} y {@link #WITHDRAWN} no mueven
 * nada y exigen lo contrario —que no haya importe ni devolucion enlazada—. Esa
 * simetria es {@code chk_prr_applied_amount}.
 */
public enum ReversalOutcome {

    ACCEPTED, PARTIALLY_ACCEPTED, REJECTED, WITHDRAWN;

    /** Los desenlaces que sacan dinero, y por tanto exigen importe aplicado. */
    public boolean movesMoney() {
        return this == ACCEPTED || this == PARTIALLY_ACCEPTED;
    }
}
