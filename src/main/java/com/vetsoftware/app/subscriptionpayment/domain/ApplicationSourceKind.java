package com.vetsoftware.app.subscriptionpayment.domain;

/**
 * De dónde sale lo que salda una factura. Espejo de
 * {@code chk_bda_source_kind}.
 *
 * <p>
 * <strong>{@link #CREDIT_NOTE} es el bloqueante que la primera versión del
 * modelo no tenía.</strong> Cuando esta tabla solo aceptaba pagos, un saldo a
 * favor no podía descontarse: el saldo de la factura no bajaba nunca, el reloj
 * de la mora seguía corriendo, y una clínica a la que se le había devuelto
 * dinero acababa en solo lectura por una deuda que ya no existía — con el
 * sistema teniendo razón según sus propios números.
 */
public enum ApplicationSourceKind {
    /** Un pago recibido: entró dinero. */
    PAYMENT,
    /** Un saldo a favor de una nota crédito: no entra un peso y salda igual. */
    CREDIT_NOTE
}
