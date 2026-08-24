package com.vetsoftware.app.subscriptionbilling.application.command;

/**
 * Pasar el documento a la cola de emisión externa:
 * {@code DRAFT → AWAITING_EXTERNAL}.
 */
public record SubmitBillingDocumentCommand(Long id, Long companyId) {
}
