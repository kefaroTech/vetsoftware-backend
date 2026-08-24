package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * Se intento agrupar cargos de los dos signos en una nota credito. HTTP 409.
 *
 * <p>
 * <b>Por que importa y por que no es un CHECK.</b> El subtotal del documento es
 * siempre positivo y la conciliacion R6 lo compara contra
 * {@code ABS(SUM(subtotal_amount))} de sus cargos. Si la nota credito mezcla un
 * cargo positivo con uno negativo, esa suma se compensa parcialmente, el
 * {@code ABS} deja de ser el subtotal y <b>la vigilancia miente sin devolver
 * ninguna fila</b>: el descuadre aparece en la declaracion bimestral, no antes.
 * Un {@code CHECK} no puede verlo porque tendria que agregar filas de otra
 * tabla.
 *
 * <p>
 * Una factura si puede mezclar signos -una cuota con su descuento- y por eso la
 * regla es solo de {@code CREDIT_NOTE}.
 */
public class MixedSignChargesException extends RuntimeException {
    public MixedSignChargesException(int positivos, int negativos) {
        super("A credit note cannot mix charges of both signs: " + positivos + " positive and "
                + negativos + " negative");
    }
}
