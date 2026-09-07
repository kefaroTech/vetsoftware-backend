package com.vetsoftware.app.quote.application.port.out;

import java.math.BigDecimal;

/**
 * Rastro de auditoría de las transiciones de la cotización.
 *
 * <p>
 * Actor y empresa viajan por el MDC — no por aquí.
 */
public interface QuoteAuditPort {

    /**
     * La oferta paso a {@code ACCEPTED} y, si tenia empresa, el contrato ya nacio
     * en la misma transaccion.
     *
     * @param subscriptionId
     *            el contrato que nacio de esta aceptacion, o {@code null} si la
     *            oferta era de un prospecto sin empresa todavia
     */
    void quoteAccepted(Long quoteId, String quoteNumber, Long companyId, Long subscriptionId,
            BigDecimal totalAmount, String currency, String acceptedByEmail, String acceptedIp);
}
