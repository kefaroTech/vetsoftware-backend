package com.vetsoftware.app.electronicdocument.domain;

/** Esquema del tributo de la linea, codigo DIAN: IVA=01, INC=04. */
public enum TaxScheme {
    IVA("01"),
    INC("04");

    private final String dianCode;

    TaxScheme(String dianCode) {
        this.dianCode = dianCode;
    }

    public String dianCode() {
        return dianCode;
    }
}
