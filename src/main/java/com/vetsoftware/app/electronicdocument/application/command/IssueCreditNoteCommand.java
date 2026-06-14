package com.vetsoftware.app.electronicdocument.application.command;

import com.vetsoftware.app.electronicdocument.domain.CreditNoteReason;

/** Emite una nota credito total que anula la factura {@code documentId} de la empresa {@code companyId}. */
public record IssueCreditNoteCommand(Long documentId, CreditNoteReason reason, Long companyId) {}
