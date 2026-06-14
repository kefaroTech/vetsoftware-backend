package com.vetsoftware.app.electronicdocument.application.command;

import com.vetsoftware.app.electronicdocument.domain.DebitNoteReason;

/** Emite una nota debito que aumenta la factura {@code documentId} de la empresa {@code companyId}. */
public record IssueDebitNoteCommand(Long documentId, DebitNoteReason reason, Long companyId) {}
