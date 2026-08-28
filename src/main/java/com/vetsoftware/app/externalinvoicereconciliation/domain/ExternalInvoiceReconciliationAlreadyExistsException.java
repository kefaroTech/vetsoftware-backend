package com.vetsoftware.app.externalinvoicereconciliation.domain;

/**
 * Ya hay una conciliacion abierta para ese documento de cobro.
 *
 * <p>
 * Espejo en Java de {@code uq_eir_document (company_id, billing_document_id)}:
 * un documento de cobro tiene <strong>una</strong> conciliacion, no dos. La
 * unicidad la cuida la base de verdad; esta excepcion existe para que el
 * segundo intento salga como un 409 explicando que ya existe, y no como el 500
 * sin explicacion en que se convierte una violacion de indice unico.
 *
 * <p>
 * Es un conflicto (409) y no un cuerpo mal formado (400): la peticion esta bien
 * escrita y lo que choca es el estado del expediente.
 */
public class ExternalInvoiceReconciliationAlreadyExistsException extends RuntimeException {

    public ExternalInvoiceReconciliationAlreadyExistsException(Long companyId,
            Long billingDocumentId) {
        super("External invoice reconciliation already exists for billing document "
                + billingDocumentId + " of company " + companyId);
    }
}
