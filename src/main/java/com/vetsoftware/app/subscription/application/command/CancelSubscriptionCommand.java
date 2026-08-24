package com.vetsoftware.app.subscription.application.command;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cancelacion. Separa <strong>cuando lo pidio</strong> de <strong>cuando surte
 * efecto</strong>: el cliente cancela el 10 y se va el 30, que es lo que ya
 * pago. Hasta el 30 el contrato sigue siendo el vigente de su empresa.
 *
 * <p>
 * <strong>No trae importes.</strong> El abono por los dias sin devengar lo
 * calcula el caso de uso sobre las lineas vigentes del contrato.
 */
public record CancelSubscriptionCommand(Long id, Long companyId, LocalDateTime requestedAt,
        LocalDate effectiveDate, String reason, String clientRequestId, Long requestedByEmployeeId,
        Long requestedBySystemUserId) {
}
