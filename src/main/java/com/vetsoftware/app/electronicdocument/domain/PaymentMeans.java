package com.vetsoftware.app.electronicdocument.domain;

/** Medio de pago, catalogo DIAN (subconjunto vigente para la veterinaria). */
public enum PaymentMeans {
    EFECTIVO("10"), TARJETA_DEBITO("48"), TARJETA_CREDITO("49"), TRANSFERENCIA("42");

    private final String dianCode;

    PaymentMeans(String dianCode) {
        this.dianCode = dianCode;
    }

    public String dianCode() {
        return dianCode;
    }
}
