package com.vetsoftware.app.electronicdocument.application.command;

import com.vetsoftware.app.electronicdocument.domain.CreditNoteReason;

/**
 * Emite una nota credito total que anula la factura {@code documentId} de la empresa {@code companyId}.
 * {@code issuedByEmployeeId} es el actor que emite la nota (trazabilidad de la anulacion), inyectado por el
 * controller desde el contexto de autenticacion.
 */
public record IssueCreditNoteCommand(Long documentId, CreditNoteReason reason, Long companyId,
                                     Long issuedByEmployeeId) {}
