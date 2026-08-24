package com.vetsoftware.app.subscriptionbilling.application.command;

import java.util.List;

/**
 * Emitir la nota crédito que corrige un documento ya registrado, encadenándola
 * por {@code correctsDocumentId}.
 *
 * <p>
 * Es el <b>único</b> camino para corregir un documento con factura externa: su
 * importe no se toca. Los cargos que agrupa tienen que ser todos del mismo
 * signo —negativo—, o el {@code ABS(SUM(...))} de la conciliación deja de ser
 * el subtotal del documento.
 */
public record IssueCreditNoteCommand(Long companyId, Long correctsDocumentId,
        List<Long> chargeIds) {
}
