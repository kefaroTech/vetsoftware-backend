package com.vetsoftware.app.smmlvvalue.domain;

/**
 * En que situacion esta la cifra del salario minimo de un ano. Espejo
 * <strong>literal</strong> de {@code chk_smmlv_values_status}.
 *
 * <p>
 * Son tres porque las tres pasaron de verdad: la cifra rige, un tribunal la
 * suspende, o una norma posterior la reemplaza. Con solo «vigente» y «no
 * vigente» no se distinguiria una suspension cautelar —donde el numero se sigue
 * usando mientras el fondo se decide— de una derogacion.
 */
public enum SmmlvStatus {

    /**
     * Rige sin discusion. Es el unico estado que la base admite sin motivo escrito.
     */
    IN_FORCE,

    /**
     * Suspendida provisionalmente por decision judicial. La cifra sigue en uso, en
     * disputa.
     */
    SUSPENDED,

    /** Reemplazada por una norma posterior. */
    SUPERSEDED
}
