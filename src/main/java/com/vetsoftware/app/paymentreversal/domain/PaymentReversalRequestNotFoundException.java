package com.vetsoftware.app.paymentreversal.domain;

/** El expediente no existe, o no es de la empresa que pregunta (404). */
public class PaymentReversalRequestNotFoundException extends RuntimeException {

    public PaymentReversalRequestNotFoundException(Long id) {
        super("Payment reversal request not found: " + id);
    }
}
