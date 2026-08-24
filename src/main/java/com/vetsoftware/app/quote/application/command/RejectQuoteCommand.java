package com.vetsoftware.app.quote.application.command;

/** Paso SENT -> REJECTED. */
public record RejectQuoteCommand(Long id, Long companyId) {
}
