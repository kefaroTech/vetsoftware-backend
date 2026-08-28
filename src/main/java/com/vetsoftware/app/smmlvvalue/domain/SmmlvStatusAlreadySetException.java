package com.vetsoftware.app.smmlvvalue.domain;

/**
 * Se pidio mover el estado al que ya tiene. Mapea a 409.
 *
 * <p>
 * No es una comprobacion cosmetica: aceptarlo reescribiria
 * {@code status_reference} y {@code status_changed_on} con los de la peticion
 * nueva, y la fecha del auto que suspendio la cifra —que es la que fija desde
 * cuando esta en disputa— quedaria pisada por la fecha de un reintento.
 */
public class SmmlvStatusAlreadySetException extends RuntimeException {

    public SmmlvStatusAlreadySetException(int fiscalYear, SmmlvStatus status) {
        super("SMMLV value for " + fiscalYear + " is already in status " + status);
    }
}
