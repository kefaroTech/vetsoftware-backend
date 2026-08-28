package com.vetsoftware.app.subscriptionpaymentmethod.application.command;

/**
 * Revocacion del mandato.
 *
 * <p>
 * La fecha no viaja en el command: la pone el servidor con el reloj inyectado.
 * Es la frontera a partir de la cual los cobros dejan de estar autorizados, y
 * dejar que la elija el cliente permitiria moverla hacia atras para
 * desautorizar cobros que si tenian mandato.
 */
public record RevokeSubscriptionPaymentMethodCommand(Long id, Long companyId, String reason) {
}
