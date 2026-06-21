package com.vetsoftware.app.electronicdocument.domain;

/**
 * Copia congelada de la identidad fiscal del adquiriente al momento de emitir.
 * No es una referencia viva a Owner: si el dueno cambia luego, el documento conserva esta copia.
 * Incluye el email del adquiriente para el envío de la representación gráfica (F4).
 */
public record CustomerSnapshot(
        String documentType,
        String documentId,
        String verificationDigit,
        String personType,
        String legalName,
        String name,
        String email,
        String cityDaneCode,
        // Régimen de IVA del adquiriente. Determina el tax_regime_id ante la DIAN. Puede ser null en
        // documentos antiguos (anteriores a capturar el régimen) → el adaptador cae a una heurística.
        TaxRegime taxRegime,
        // Responsabilidad fiscal del adquiriente. Determina el tax_level_id (TaxLevelCode) ante la DIAN.
        // null en documentos antiguos (anteriores a capturarla) → el adaptador cae al default (NO_APLICA).
        FiscalResponsibility fiscalResponsibility
) {
    public CustomerSnapshot {
        if (documentId == null || documentId.isBlank())
            throw new IllegalArgumentException("customer documentId is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("customer name is required");
    }

    /** NIT genérico DIAN para ventas a un adquiriente que no se identifica ("consumidor final"). */
    public static final String FINAL_CONSUMER_DOCUMENT = "222222222222";

    /**
     * Identidad fiscal del "consumidor final": adquiriente genérico para emitir sin identificar al cliente,
     * sin crear un Owner con el NIT genérico (chocaría con unique(company_id, document)).
     * documentType/personType se guardan como String igual que el snapshot normal (sin acoplar al enum de owner).
     * TODO: confirmar contra el sandbox el código de tipo de documento que el proveedor espera para consumidor final.
     */
    public static CustomerSnapshot finalConsumer() {
        return new CustomerSnapshot(
                "CEDULA_CIUDADANIA", // código DIAN 13
                FINAL_CONSUMER_DOCUMENT,
                null,
                "NATURAL",
                null,
                "Consumidor final",
                null,
                null,
                TaxRegime.NO_RESPONSABLE_IVA, // el consumidor final no es responsable de IVA
                FiscalResponsibility.NO_APLICA); // R-99-PN: el consumidor final no tiene responsabilidad especial
    }
}
