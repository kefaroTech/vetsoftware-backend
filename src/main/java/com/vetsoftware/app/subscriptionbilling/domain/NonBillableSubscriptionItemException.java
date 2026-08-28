package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * Se intento devengar un cargo sobre una linea que no cobra. HTTP 409.
 *
 * <p>
 * <b>Por que es una excepcion propia y no un {@code IllegalArgumentException}
 * mas.</b> El id que llego es correcto, la linea existe y es de la empresa: lo
 * que no puede es devengar. Es un conflicto de estado, no un dato mal escrito,
 * y el operador de plataforma necesita distinguirlo -- si lo unico que ve es
 * "argumento invalido" vuelve a intentarlo con el mismo id.
 *
 * <p>
 * <b>El mensaje nombra el modo a proposito.</b> El fallo tipico es un cierre
 * mensual que devenga contra la linea en prueba de un cliente; saber que estaba
 * en {@code TRIAL} y no en {@code FREE_LIMITED} es la diferencia entre esperar
 * a la conversion y revisar el tope del plan. Ver {@link ItemChargeMode} para
 * por que la linea gratuita conserva su tarifa real.
 */
public class NonBillableSubscriptionItemException extends RuntimeException {

    public NonBillableSubscriptionItemException(Long subscriptionItemId,
            ItemChargeMode chargeMode) {
        super("Subscription item " + subscriptionItemId + " has charge_mode " + chargeMode
                + " and does not accrue: only PAID lines are billed, and the price stored on a"
                + " free line is the one that will apply after the trial, not zero");
    }
}
