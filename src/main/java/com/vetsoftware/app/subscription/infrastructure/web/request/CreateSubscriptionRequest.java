package com.vetsoftware.app.subscription.infrastructure.web.request;

import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.List;

/**
 * Alta de contrato. <strong>Sin {@code companyId}</strong>: la empresa la pone
 * el controller con {@code authz.currentCompanyId()}, nunca el cliente.
 *
 * <p>
 * <strong>Y sin numero de contrato.</strong> {@code subscription_number} es el
 * identificador que se cita en soporte y en cobranza, y un numero citable que
 * escribe el cliente de la API no es citable: lo reserva el servidor de forma
 * serializada, dentro de la misma transaccion del alta.
 *
 * <p>
 * {@code @Valid} en {@code items} no es decorativo: sin el, las restricciones
 * de {@link RequestedSubscriptionItemRequest} no se evaluan aunque esten
 * escritas.
 */
public record CreateSubscriptionRequest(Long quoteId, @NotNull Long priceListId,
        @NotNull BillingCycle billingCycle, @NotNull SubscriptionStatus status,
        @NotNull LocalDate startDate, LocalDate trialEndDate, @NotNull LocalDate currentPeriodStart,
        @NotNull LocalDate currentPeriodEnd, LocalDate nextBillingDate, LocalDate commitmentEndDate,
        @PositiveOrZero Integer graceDays, Boolean autoRenew,
        @Valid List<RequestedSubscriptionItemRequest> items) {
}
