package com.vetsoftware.app.electronicdocument.application.command;

import com.vetsoftware.app.electronicdocument.domain.DebitNoteReason;
import java.math.BigDecimal;

/**
 * Emite una nota debito que aumenta la factura {@code documentId} de la empresa {@code companyId}.
 * {@code issuedByEmployeeId} es el actor que emite la nota, inyectado por el controller desde el
 * contexto auth. {@code additionalAmount} null ⇒ clona el original (heredado); con un monto (&gt;
 * 0) ⇒ ese incremento real.
 */
public record IssueDebitNoteCommand(
    Long documentId,
    DebitNoteReason reason,
    Long companyId,
    Long issuedByEmployeeId,
    BigDecimal additionalAmount) {}
