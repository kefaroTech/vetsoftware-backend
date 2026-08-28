package com.vetsoftware.app.subscriptionpaymentmethod.application.command;

/**
 * Marca caducado un mandato de tarjeta. Lo dispara el barrido de plataforma que
 * recorre {@code ix_subscription_payment_methods_expiring}, no el cliente.
 *
 * <p>
 * Lleva {@code companyId} aunque el caso de uso este cerrado a plataforma: la
 * carga por id va acotada igual, para que un id equivocado no pueda alcanzar la
 * fila de otra clinica ni siquiera desde un proceso interno.
 */
public record ExpireSubscriptionPaymentMethodCommand(Long id, Long companyId) {
}
