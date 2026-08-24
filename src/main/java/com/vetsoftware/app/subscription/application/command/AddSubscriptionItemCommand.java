package com.vetsoftware.app.subscription.application.command;

import java.time.LocalDate;

/**
 * Alta de linea. Genera su otrosi {@code ADD_ITEM} y comprueba el solape antes
 * de abrir. {@code clientRequestId} es la llave antiduplicados: se busca antes
 * de insertar, dentro de la transaccion.
 *
 * <p>
 * <strong>No trae importes.</strong> El prorrateo y el cambio de la cuota
 * recurrente los calcula el caso de uso; que no esten aqui es lo que garantiza
 * que ningun caller —ni el controller, ni otro servicio futuro— pueda
 * dictarlos.
 */
public record AddSubscriptionItemCommand(Long id, Long companyId, String clientRequestId,
        LocalDate effectiveDate, String reason, Long requestedByEmployeeId,
        Long requestedBySystemUserId, Long quoteId, SubscriptionItemLineCommand line) {
}
