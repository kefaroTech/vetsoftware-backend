package com.vetsoftware.app.paymentreversal.domain;

/**
 * Ya hay un expediente abierto sobre ese pago (409).
 *
 * <p>
 * Es {@code uq_payment_reversal_requests_payment} escrito en el dominio: una
 * reversion por pago. La constraint convierte el duplicado en un error, pero un
 * 500 en la cara de quien opera no dice cual es el problema; la busqueda previa
 * dentro de la transaccion si.
 */
public class ReversalRequestAlreadyExistsException extends RuntimeException {

    public ReversalRequestAlreadyExistsException(Long paymentId) {
        super("A payment reversal request already exists for payment " + paymentId);
    }
}
