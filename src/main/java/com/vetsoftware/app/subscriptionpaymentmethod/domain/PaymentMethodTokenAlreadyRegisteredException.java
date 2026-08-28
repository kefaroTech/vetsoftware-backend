package com.vetsoftware.app.subscriptionpaymentmethod.domain;

/**
 * El par {@code (gateway, token)} ya esta registrado (409).
 *
 * <p>
 * <strong>El mensaje no dice de quien es, y eso es la mitad de su
 * trabajo.</strong> {@code uq_subscription_payment_methods_token} es una
 * unicidad <em>global</em>, no por empresa, asi que la fila que colisiona puede
 * ser de otra clinica. Nombrarla —o devolverla— convertiria un conflicto en una
 * fuga: bastaria probar testigos para averiguar quien mas usa esa pasarela.
 */
public class PaymentMethodTokenAlreadyRegisteredException extends RuntimeException {

    public PaymentMethodTokenAlreadyRegisteredException(String gateway) {
        super("Payment method token is already registered for gateway: " + gateway);
    }
}
