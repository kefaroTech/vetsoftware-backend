package com.vetsoftware.app.electronicdocument.domain;

/**
 * Forma de pago DIAN: 1 contado. El crédito (2) no está soportado: toda venta
 * es de contado.
 */
public enum PaymentForm {
    CONTADO("1");

    private final String dianCode;

    PaymentForm(String dianCode) {
        this.dianCode = dianCode;
    }

    public String dianCode() {
        return dianCode;
    }
}
