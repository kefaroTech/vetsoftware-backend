package com.vetsoftware.app.electronicdocument.domain;

/**
 * Concepto de correccion de la nota credito (catalogo DIAN §2.5). El codigo
 * viaja en el XML UBL (DiscrepancyResponse/ResponseCode). NO confundir con el
 * catalogo de la nota debito (DebitNoteReason).
 */
public enum CreditNoteReason {
    DEVOLUCION("1", "Devolución parcial o total de bienes/servicios"), ANULACION("2",
            "Anulación de factura electrónica"), REBAJA("3",
                    "Rebaja o descuento parcial o total"), AJUSTE_PRECIO("4",
                            "Ajuste de precio"), OTROS("5", "Otros");

    private final String dianCode;
    private final String description;

    CreditNoteReason(String dianCode, String description) {
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
