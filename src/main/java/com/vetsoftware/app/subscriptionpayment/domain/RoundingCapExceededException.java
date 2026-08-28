package com.vetsoftware.app.subscriptionpayment.domain;

import java.math.BigDecimal;

/**
 * El residuo de redondeo pedido pasa del tope.
 *
 * <p>
 * <b>El tope es lo unico que separa este origen de un vertedero.</b> Sin el,
 * {@code ROUNDING} salda cualquier cosa: un descuadre de doscientos mil pesos
 * entra igual de bien que uno de dos, la cartera cuadra, y el error que lo
 * produjo desaparece sin dejar rastro. Con el tope, un descuadre grande sigue
 * vivo y visible, que es exactamente donde tiene que estar.
 *
 * <p>
 * Espejo de {@code chk_bda_rounding_cap} ({@code ABS(applied_amount) <= 3}). La
 * base lo rechazaria tambien, pero como una violacion de constraint convertida
 * en 500; aqui llega como 400 con el numero delante.
 */
public class RoundingCapExceededException extends IllegalArgumentException {

    public RoundingCapExceededException(BigDecimal requested, BigDecimal cap) {
        super("Un residuo de redondeo no puede pasar de " + cap + " y se pidieron " + requested
                + ": ese importe no es redondeo, es un descuadre que hay que explicar."
                + " Saldarlo por aqui lo haria desaparecer sin dejar rastro.");
    }
}
