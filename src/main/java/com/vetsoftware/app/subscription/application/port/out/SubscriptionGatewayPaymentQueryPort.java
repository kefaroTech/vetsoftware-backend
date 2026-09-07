package com.vetsoftware.app.subscription.application.port.out;

import java.time.LocalDate;

/**
 * Lo que el estado de la pasarela dice del contrato.
 */
public interface SubscriptionGatewayPaymentQueryPort {

    /**
     * ¿Hay un pago {@code PENDING} aplicado a algun documento del contrato?
     * Mientras lo esté, sustituirlo dejaría esa resolución apuntando a un contrato
     * que ya no es el vigente de la empresa.
     */
    boolean existsPendingPayment(Long companyId, Long subscriptionId);

    /**
     * ¿El periodo exacto ya tiene un pago {@code CONFIRMED} aplicado a su
     * documento, no anulado? Solo entonces hay algo que devolver al sustituir el
     * contrato antes de que ese periodo termine.
     */
    boolean isPeriodPaid(Long companyId, Long subscriptionId, LocalDate periodStart,
            LocalDate periodEnd);
}
