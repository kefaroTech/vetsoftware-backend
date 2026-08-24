package com.vetsoftware.app.subscriptionbilling.application.command;

import java.time.LocalDate;

/**
 * Capturar aquí la referencia de la factura que se emitió <b>fuera</b>.
 *
 * <p>
 * {@code issuedAt} es <b>la fecha fiscal</b>, y es desde ella —no desde el
 * cálculo interno— desde donde se cuenta el vencimiento. El CUFE puede llegar
 * en un segundo paso y por eso es opcional.
 */
public record RegisterExternalInvoiceCommand(Long id, Long companyId, String invoiceNumber,
        String cufe, LocalDate issuedAt, String provider, Long registeredBySystemUserId) {
}
