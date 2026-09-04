package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * Se intento cambiar algo que un documento con factura externa ya registrada no
 * puede cambiar. HTTP 409, errorCode {@code DOCUMENT_ALREADY_ISSUED}.
 *
 * <p>
 * <b>Que se rompe si esto no existe.</b> Lo que dice Lumbre deja de coincidir
 * con lo que tiene la DIAN y no hay forma de saber cual de los dos miente.
 * Cuando {@code issue_status = 'EXTERNAL_REGISTERED'} solo cambian lo saldado,
 * el saldo -que es columna calculada- y el estado; el importe, el periodo y el
 * tipo quedan sellados. Corregirlos exige una nota credito emitida fuera y
 * registrada aqui, encadenada al original por {@code corrects_document_id}.
 *
 * <p>
 * R2 de {@code suscripciones-reglas-codigo.md}.
 */
public class BillingDocumentAlreadyIssuedException extends RuntimeException {
    public BillingDocumentAlreadyIssuedException(Long id, String intento) {
        super("Billing document " + id + " has an external invoice registered and cannot " + intento
                + ": issue a credit note chained by correctsDocumentId instead");
    }
}
