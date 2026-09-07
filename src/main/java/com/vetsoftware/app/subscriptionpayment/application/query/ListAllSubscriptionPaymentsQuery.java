package com.vetsoftware.app.subscriptionpayment.application.query;

import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import java.time.LocalDateTime;

/**
 * Filtro de tesorería cross-tenant. {@code pendingOlderThanMinutes} manda sobre
 * {@code status}: cuando llega, el barrido busca PENDING de pasarela
 * envejecidos y descarta cualquier otro estado pedido a la vez, porque las dos
 * cosas juntas no tienen lectura razonable.
 */
public record ListAllSubscriptionPaymentsQuery(Long companyId, SubscriptionPaymentStatus status,
        LocalDateTime receivedFrom, LocalDateTime receivedTo, Integer pendingOlderThanMinutes,
        int page, int pageSize) {
}
