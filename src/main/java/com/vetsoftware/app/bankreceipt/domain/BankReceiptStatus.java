package com.vetsoftware.app.bankreceipt.domain;

/**
 * En que punto de la conciliacion esta una entrada del extracto. Dominio
 * cerrado y espejo <strong>literal</strong> de
 * {@code chk_bank_receipts_status}: los tres nombres se escriben aqui igual que
 * en la constraint, porque {@code @Enumerated(EnumType.STRING)} guarda el
 * {@code name()} tal cual y un valor que la comprobacion no admita lo rechaza
 * la base con un error que no menciona ni la columna ni el valor.
 *
 * <p>
 * <strong>No hay un cuarto estado ni un {@code enabled} que lo
 * acompañe.</strong> Una entrada de extracto no se desactiva: si resulta que no
 * era una consignacion de un cliente —una nota del banco, un traslado interno—
 * se marca {@link #DISCARDED} y <em>queda</em>. El extracto es un espejo de lo
 * que hizo el banco y borrar una linea, aunque sea en logico, deja el cuadre
 * sin la mitad de su explicacion.
 */
public enum BankReceiptStatus {

    /**
     * Recien cargada del extracto y sin dueño. Es el estado inicial y el unico que
     * la base admite con {@code identified_at} nulo.
     */
    UNIDENTIFIED,

    /** Ya se sabe de quien era la consignacion. */
    IDENTIFIED,

    /** No corresponde a ningun cliente y se saca de la bandeja sin borrarla. */
    DISCARDED
}
