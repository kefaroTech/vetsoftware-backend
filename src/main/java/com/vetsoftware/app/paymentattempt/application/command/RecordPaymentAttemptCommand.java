package com.vetsoftware.app.paymentattempt.application.command;

import com.vetsoftware.app.paymentattempt.domain.DeclineKind;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <strong>Sin {@code attemptNumber}</strong>: no lo elige quien llama. Lo
 * calcula el caso de uso dentro de la transaccion como el siguiente del
 * documento, porque {@code uq_payment_attempts_number} lo quiere consecutivo y
 * dejarselo al cliente es como se cuelan huecos y colisiones.
 *
 * @param nextAttemptAt
 *            cuando se reintenta. En un {@link DeclineKind#HARD} va
 *            <strong>vacio</strong>: no hay siguiente hasta que aparezca otra
 *            tarjeta
 */
public record RecordPaymentAttemptCommand(Long companyId, Long billingDocumentId,
        Long paymentMethodId, String gateway, BigDecimal requestedAmount, String gatewayDeclineCode,
        DeclineKind declineKind, LocalDateTime attemptedAt, LocalDateTime nextAttemptAt) {
}
