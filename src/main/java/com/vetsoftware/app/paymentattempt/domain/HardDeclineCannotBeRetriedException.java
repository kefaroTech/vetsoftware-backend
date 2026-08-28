package com.vetsoftware.app.paymentattempt.domain;

/**
 * Reprogramar un rechazo duro. No es un error de datos sino un conflicto con el
 * estado del intento (409): la tarjeta está perdida, robada o su autorización
 * revocada, y volver a pasarla no puede salir bien.
 *
 * <p>
 * El motor lo impide igual con {@code chk_payment_attempts_hard_has_no_retry},
 * pero llegar hasta ahí convertiría una regla de negocio explicable en un error
 * de integridad sin mensaje. La salida correcta es pedir medio de pago nuevo.
 */
public class HardDeclineCannotBeRetriedException extends RuntimeException {

    public HardDeclineCannotBeRetriedException(Long id) {
        super("Hard decline cannot be retried, a new payment method is required: " + id);
    }
}
