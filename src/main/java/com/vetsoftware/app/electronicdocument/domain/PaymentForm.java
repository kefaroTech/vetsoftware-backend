package com.vetsoftware.app.electronicdocument.domain;

/** Forma de pago DIAN: 1 contado, 2 credito. */
public enum PaymentForm {
    CONTADO("1"),
    CREDITO("2");

    private final String dianCode;

    PaymentForm(String dianCode) {
        this.dianCode = dianCode;
    }

    public String dianCode() {
        return dianCode;
    }
}
