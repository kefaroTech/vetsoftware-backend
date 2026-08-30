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
 * @param aiProposalId
 *            id de la propuesta del asistente de la que salio esta oferta, o
 *            null. Es el <b>id</b> y nunca el token publico: el token es el
 *            secreto de la URL y copiarlo a una segunda tabla lo sacaria del
 *            control de acceso que lo protege. Lo resuelve
 *            {@code SelfServeQuoteService} contra
 *            {@code ProposalReferencePort}; el camino de plataforma no lo usa,
 *            porque la consola no cotiza desde propuestas.
 */
public record CreateQuoteCommand(String clientRequestId, Long companyId, String prospectName,
        String prospectEmail, String prospectDocument, String prospectPhone, Long priceListId,
        String billingCycle, LocalDate validUntil, int trialDays, List<QuoteLineCommand> lines,
        Long aiProposalId) {

    /**
     * Sin propuesta detras: el camino de plataforma, y todo lo que existia antes de
     * DC-2. Es un constructor secundario y no un valor por defecto para que anadir
     * la atribucion no obligara a tocar los nueve sitios que ya construian este
     * command.
     */
    public CreateQuoteCommand(String clientRequestId, Long companyId, String prospectName,
            String prospectEmail, String prospectDocument, String prospectPhone, Long priceListId,
            String billingCycle, LocalDate validUntil, int trialDays,
            List<QuoteLineCommand> lines) {
        this(clientRequestId, companyId, prospectName, prospectEmail, prospectDocument,
                prospectPhone, priceListId, billingCycle, validUntil, trialDays, lines, null);
    }
}
