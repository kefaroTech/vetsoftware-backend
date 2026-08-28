package com.vetsoftware.app.gatewaysettlement.domain;

/**
 * El contraste entre lo que la liquidacion <em>dice</em> que trae y los cobros
 * que de verdad estan enlazados a ella.
 *
 * <p>
 * <strong>Es la razon de ser de {@code payment_count}.</strong> Sin esa columna
 * el lote solo sabe cuanto dinero movio, y un cobro que la pasarela liquido
 * pero que nunca se ato a su lote no deja ningun rastro: el dinero cuadra
 * —entro en el bruto— pero el cobro del cliente queda sin conciliar y la
 * clinica aparece debiendo lo que ya pago. Declarar la cuenta convierte esa
 * revision a ojo, que hoy se hace todos los meses, en una consulta: <b>si dice
 * 37 y hay 36, hay un pago perdido.</b>
 *
 * <p>
 * <strong>El signo de la diferencia dice cual de los dos defectos es</strong>,
 * y son distintos:
 *
 * <ul>
 * <li><b>Positiva</b> (declara mas de los que hay): falta atar un cobro. Es el
 * caso caro — hay un cliente cuyo pago no se ve.</li>
 * <li><b>Negativa</b> (hay mas de los que declara): se ato un cobro que no
 * pertenece a este lote, o la pasarela reenvio la liquidacion con otra cuenta.
 * El dinero del lote no cubre los cobros atados y el cuadre da de mas.</li>
 * </ul>
 *
 * <p>
 * <strong>No lanza cuando no cuadra.</strong> Un descuadre es un hallazgo que
 * hay que poder listar y explicar, no un error que aborte la consulta: el
 * proposito de esta rodaja es justamente <em>enseñar</em> las liquidaciones
 * descuadradas, y una excepcion dejaria la peor —la unica que importa— sin
 * poderse mirar.
 */
public record PaymentCountReconciliation(int declared, long linked) {

    public PaymentCountReconciliation {
        // Espejo de chk_gateway_settlements_payment_count: un lote de cero cobros
        // no es un lote.
        if (declared <= 0)
            throw new IllegalArgumentException("declared payment count must be greater than zero");
        if (linked < 0)
            throw new IllegalArgumentException("linked payment count cannot be negative");
    }

    /** Cobros declarados menos cobros enlazados. Cero es el unico valor sano. */
    public long difference() {
        return declared - linked;
    }

    public boolean isBalanced() {
        return difference() == 0;
    }
}
