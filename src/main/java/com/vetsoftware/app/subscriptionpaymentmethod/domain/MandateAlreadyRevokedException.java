package com.vetsoftware.app.subscriptionpaymentmethod.domain;

/**
 * Se intento revocar o caducar un mandato ya revocado (409).
 *
 * <p>
 * No se trata como idempotente a proposito. La revocacion lleva fecha y motivo,
 * y son los dos datos con los que se prueba <em>cuando</em> dejo de haber
 * autorizacion; reescribirlos con los de un segundo intento mueve esa frontera
 * y con ella la de que cobros estaban autorizados. Conservarlos en silencio
 * tampoco vale: quien revoca por segunda vez cree haber cambiado el motivo.
 */
public class MandateAlreadyRevokedException extends RuntimeException {

    public MandateAlreadyRevokedException(Long id) {
        super("Subscription payment method mandate is already revoked: " + id);
    }
}
