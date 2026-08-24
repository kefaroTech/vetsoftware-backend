package com.vetsoftware.app.subscription.application.command;

import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * Alta de contrato con sus lineas iniciales, en una sola transaccion (R10: toda
 * empresa nace con un contrato).
 *
 * <p>
 * No trae numero de contrato: {@code subscription_number} lo reserva el
 * servidor de forma serializada. {@code companyId} lo inyecta el controller
 * desde el principal, nunca el cuerpo de la peticion.
 */
public record CreateSubscriptionCommand(Long companyId, Long quoteId, Long priceListId,
        BillingCycle billingCycle, SubscriptionStatus status, LocalDate startDate,
        LocalDate trialEndDate, LocalDate currentPeriodStart, LocalDate currentPeriodEnd,
        LocalDate nextBillingDate, LocalDate commitmentEndDate, Integer graceDays,
        Boolean autoRenew, String actor, List<SubscriptionItemLineCommand> items) {
}
