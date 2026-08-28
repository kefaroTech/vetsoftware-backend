package com.vetsoftware.app.entitlement.domain;

import java.math.BigDecimal;

/**
 * <strong>El permiso de pasarse, tal como lo declaro el contrato.</strong>
 * Companion VO propio de esta rodaja: la fuente del dato es
 * {@code subscription_item_limits} —de otra feature— y aqui entra por un puerto
 * de salida, nunca importando su dominio.
 *
 * <p>
 * Su ausencia es la respuesta por defecto y por eso el puerto devuelve
 * {@code Optional}: <b>sin permiso escrito, se bloquea</b>. Solo la linea de
 * contrato que declaro {@code enforcement = OVERAGE} <em>y</em> un precio por
 * unidad positivo puede pasar del techo, que es exactamente lo que dice
 * {@code chk_subscription_item_limits_overage}.
 *
 * <p>
 * <strong>Los tres campos son los tres que hacen falta para devengar el
 * cargo</strong>, y ninguno mas: la linea del contrato (de la que cuelga el
 * cargo por clave foranea compuesta), el contrato al que pertenece, y cuanto
 * vale cada unidad de exceso. El precio viaja <b>copiado</b> y no referenciado
 * —dentro de un año la tarifa habra cambiado y el cargo ya emitido tiene que
 * seguir explicandose solo—.
 *
 * @param subscriptionItemId
 *            la linea del contrato que vendio el cupo. <b>La linea, no el
 *            articulo</b>: con tramos acumulativos un mismo articulo tiene dos
 *            lineas vivas a tarifas distintas
 * @param subscriptionId
 *            el contrato del que cuelga esa linea
 * @param unitAmount
 *            precio por unidad de exceso, siempre mayor que cero
 */
public record OverageAllowance(Long subscriptionItemId, Long subscriptionId,
        BigDecimal unitAmount) {

    public OverageAllowance {
        if (subscriptionItemId == null)
            throw new IllegalArgumentException("subscription item id is required");
        if (subscriptionId == null)
            throw new IllegalArgumentException("subscription id is required");
        if (unitAmount == null || unitAmount.signum() <= 0)
            throw new IllegalArgumentException("an overage allowance needs a positive unit price:"
                    + " chk_subscription_item_limits_overage already refuses to store one"
                    + " without it, so reading a null here means the row was written around"
                    + " the schema");
    }

    /** Lo que cuesta un exceso de {@code units} unidades. */
    public BigDecimal amountFor(int units) {
        if (units <= 0)
            throw new IllegalArgumentException("overage units must be greater than zero");
        return unitAmount.multiply(BigDecimal.valueOf(units));
    }
}
