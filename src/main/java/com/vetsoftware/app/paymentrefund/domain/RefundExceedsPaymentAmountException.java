package com.vetsoftware.app.paymentrefund.domain;

import java.math.BigDecimal;

/**
 * La suma de devoluciones sobre un pago supero el importe original.
 *
 * <p>
 * <strong>Esta regla no la puede cuidar la base y el changeset 320 lo declara
 * expresamente</strong>: MySQL prohibe subconsultas dentro de un {@code CHECK},
 * asi que no hay forma de expresar «la suma de las filas hermanas mas esta no
 * pasa del pago». Queda como regla de aplicacion — y como la unicidad no la
 * protege, el service toma antes un bloqueo pesimista sobre la fila del pago:
 * sin el, dos devoluciones parciales concurrentes leen la misma suma, las dos
 * pasan la comprobacion y entre las dos devuelven mas de lo que entro.
 *
 * <p>
 * Es un conflicto (409), no una peticion mal formada: el cuerpo es valido y lo
 * que falla es el estado del pago en este instante.
 */
public class RefundExceedsPaymentAmountException extends RuntimeException {

    public RefundExceedsPaymentAmountException(Long paymentId, BigDecimal paymentAmount,
            BigDecimal alreadyRefunded, BigDecimal requested) {
        super("Refund exceeds payment amount for payment " + paymentId + ": payment is "
                + paymentAmount + ", already refunded " + alreadyRefunded + ", requested "
                + requested);
    }
}
