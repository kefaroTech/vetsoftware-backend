package com.vetsoftware.app.subscription.application.command;

/**
 * Que contrato hay que cobrar y activar.
 *
 * <p>
 * Lleva {@code companyId} porque toda carga por id de esta rodaja va acotada
 * por empresa —{@code SubscriptionRepository} no declara ninguna variante
 * ancha, a proposito— y porque el cobro tiene que saber a quien se le cobra.
 */
public record SettleNewContractCommand(Long subscriptionId, Long companyId) {
}
