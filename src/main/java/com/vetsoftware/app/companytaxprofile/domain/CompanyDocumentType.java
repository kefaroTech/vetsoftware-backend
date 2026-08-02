package com.vetsoftware.app.companytaxprofile.domain;

public enum CompanyDocumentType {
    NIT(31), CEDULA_CIUDADANIA(13), CEDULA_EXTRANJERIA(22), PASAPORTE(41);

    private final int dianCode;

    CompanyDocumentType(int dianCode) {
        this.dianCode = dianCode;
    }

    public int dianCode() {
        return dianCode;
    }
}
