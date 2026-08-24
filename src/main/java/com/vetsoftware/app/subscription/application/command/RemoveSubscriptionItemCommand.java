package com.vetsoftware.app.subscription.application.command;

import java.time.LocalDate;

/**
 * Baja de linea: escribe {@code effective_to} y emite el otrosi
 * {@code REMOVE_ITEM}. <strong>Nunca borra ni desactiva nada</strong> (R12).
 *
 * <p>
 * <strong>No trae importes.</strong> El abono lo calcula el caso de uso con el
 * precio congelado en la propia fila que se cierra.
 */
public record RemoveSubscriptionItemCommand(Long id, Long companyId, Long subscriptionItemId,
        String clientRequestId, LocalDate effectiveDate, String reason, Long requestedByEmployeeId,
        Long requestedBySystemUserId) {
}
