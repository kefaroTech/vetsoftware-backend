package com.vetsoftware.app.quote.application.command;

import java.time.LocalDate;
import java.util.List;

/**
 * Alta de una cotizacion.
 *
 * @param clientRequestId
 *            llave de idempotencia. Se busca ANTES de insertar: si el navegador
 *            reenvia la peticion no nacen dos cotizaciones y el segundo intento
 *            recibe la misma que el primero, no un 500 de clave duplicada.
 * @param companyId
 *            empresa destinataria, o null si todavia es un prospecto. Lo
 *            inyecta el controller desde el principal; NUNCA viaja en el cuerpo
 *            REST.
 */
public record CreateQuoteCommand(String clientRequestId, Long companyId, String prospectName,
        String prospectEmail, String prospectDocument, String prospectPhone, Long priceListId,
        String billingCycle, LocalDate validUntil, int trialDays, List<QuoteLineCommand> lines) {
}
