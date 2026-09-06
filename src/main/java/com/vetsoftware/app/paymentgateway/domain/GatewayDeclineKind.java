package com.vetsoftware.app.paymentgateway.domain;

/**
 * Espejo local de {@code paymentattempt.domain.DeclineKind}, con los mismos
 * tres valores. Existe para que {@code WompiDeclineClassifier} —que es
 * {@code domain} puro— no dependa del dominio de otra feature; el adaptador de
 * orquestación que sí puede verla ({@code PaymentAttemptRecorderAdapter}) la
 * traduce por nombre.
 */
public enum GatewayDeclineKind {
    SOFT, HARD, CONFIGURATION
}
