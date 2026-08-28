package com.vetsoftware.app.externalinvoicereconciliation.domain;

import java.time.LocalDateTime;

/**
 * La conciliacion ya se cerro: tiene firma, nota y periodo contable.
 *
 * <p>
 * <strong>Volver a resolverla no es idempotente, es reescribir el
 * expediente.</strong> Los cuatro campos de la resolucion van juntos
 * ({@code chk_eir_resolved}), y el {@code posting_period} decide en que periodo
 * se imputo el ajuste; cambiarlo despues mueve plata de un cierre a otro sin
 * dejar rastro de que lo hizo ni de por que. Si la resolucion estaba mal, eso
 * es una correccion contable y necesita a un humano.
 *
 * <p>
 * Es un conflicto (409).
 */
public class ExternalInvoiceReconciliationAlreadyResolvedException extends RuntimeException {

    public ExternalInvoiceReconciliationAlreadyResolvedException(Long id,
            LocalDateTime resolvedAt) {
        super("External invoice reconciliation " + id + " was already resolved at " + resolvedAt);
    }
}
