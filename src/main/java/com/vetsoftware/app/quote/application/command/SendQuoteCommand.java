package com.vetsoftware.app.quote.application.command;

/**
 * Paso DRAFT -> SENT. companyId null = camino SYSTEM sobre una oferta a
 * prospecto.
 */
public record SendQuoteCommand(Long id, Long companyId) {
}
