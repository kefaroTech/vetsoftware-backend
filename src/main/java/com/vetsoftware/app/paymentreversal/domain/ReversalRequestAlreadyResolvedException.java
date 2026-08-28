package com.vetsoftware.app.paymentreversal.domain;

/**
 * Se intento tocar un expediente que ya tiene desenlace (409).
 *
 * <p>
 * Un expediente resuelto es un hecho consumado: acusar recibo, oponerse o
 * volver a resolver despues reescribiria el pasado del que depende la defensa
 * frente a un tercero. Corregir una resolucion equivocada es un acto propio con
 * su propia constancia, nunca un {@code UPDATE} encima.
 */
public class ReversalRequestAlreadyResolvedException extends RuntimeException {

    public ReversalRequestAlreadyResolvedException(Long id, ReversalOutcome outcome) {
        super("Payment reversal request " + id + " is already resolved as " + outcome);
    }
}
