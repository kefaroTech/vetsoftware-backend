package com.vetsoftware.app.quote.application.port.out;

import java.time.LocalDate;

/**
 * Abre una línea {@code PAID} hoy por el otrosí que ya amplía contratos
 * mid-ciclo ({@code AddSubscriptionItemUseCase}), que resuelve precio contra la
 * tarifa del contrato y devenga el prorrateo. No cobra hoy: el otrosí deja un
 * {@code SubscriptionCharge} en {@code PENDING}, sin documento ni cargo a la
 * pasarela; el primer cobro real llega en el siguiente corte de
 * {@code RunSubscriptionBillingCycleService}
 * ({@code subscriptions.next_billing_date}), que es lo que el llamador debe
 * reportar como {@code firstChargeDate}. Cubre los dos casos sin línea
 * {@code TRIAL} vigente: {@code NEVER_FREE} (sin línea previa) y prueba ya
 * vencida (línea previa que el llamador cierra antes).
 */
public interface ImmediatePaidLineOpeningPort {

    void openNow(Long companyId, Long subscriptionId, Long catalogItemId, Long employeeId,
            Long quoteId, String clientRequestId, LocalDate today);
}
