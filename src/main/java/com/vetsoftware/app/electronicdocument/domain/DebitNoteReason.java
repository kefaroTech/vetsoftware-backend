package com.vetsoftware.app.electronicdocument.domain;

/**
 * Concepto de correccion de la nota debito (catalogo DIAN §2.5), distinto al de
 * la nota credito. El codigo viaja en el XML UBL
 * (DiscrepancyResponse/ResponseCode).
 */
public enum DebitNoteReason {
    INTERESES("1", "Intereses"), GASTOS("2", "Gastos por cobrar"), CAMBIO_VALOR("3",
            "Cambio del valor"), OTROS("4", "Otros");

    private final String dianCode;
    private final String description;

    DebitNoteReason(String dianCode, String description) {
        this.dianCode = dianCode;
        this.description = description;
    }

    public String dianCode() {
        return dianCode;
    }

    public String description() {
        return description;
    }
}
