package com.vetsoftware.app.entitlement.domain;

import java.time.LocalDate;

/**
 * Linea de capacidad del contrato: el techo contratado de un eje.
 *
 * <p>
 * El techo de la linea es {@code included_quantity + quantity} porque el
 * configurador ya resto lo incluido antes de fijar la cantidad (R15): lo
 * incluido viene congelado al firmar y lo comprado aparte es {@code quantity}.
 * Sumarlos aqui es lo que hace que bajar de plan sea registrable sin destruir
 * nada.
 *
 * <p>
 * La linea nombra el eje por referencia al catalogo ({@link LimitDimensionRef})
 * y no por un enumerado cerrado: es lo que permite que un eje nuevo llegue al
 * contador sin tocar codigo.
 *
 * <p>
 * <strong>{@code resetPeriod} viaja con la linea y no con el eje.</strong> Cada
 * cuanto vuelve a empezar un cupo de flujo es propiedad de la venta: el mismo
 * eje de citas se vende mensual a una clinica pequeña y semestral a una grande.
 * Llega resuelto desde el techo congelado del contrato
 * ({@code subscription_item_limits.reset_period}) y, si esa fila no existe
 * todavia, desde el techo de fabrica del articulo
 * ({@code catalog_item_limits.reset_period}) --que es la precedencia de
 * R-LIMIT-06 sin la excepcion negociada, que no declara granularidad--.
 */
public record CapacityGrantLine(Long subscriptionItemId, LimitDimensionRef dimension, int quantity,
        int includedQuantity, ResetPeriod resetPeriod, LocalDate effectiveFrom,
        LocalDate effectiveTo) {

    public CapacityGrantLine {
        if (subscriptionItemId == null)
            throw new IllegalArgumentException("subscription item id is required");
        if (dimension == null)
            throw new IllegalArgumentException("limit dimension is required");
        if (quantity <= 0)
            throw new IllegalArgumentException("capacity quantity must be positive");
        if (includedQuantity < 0)
            throw new IllegalArgumentException("included quantity cannot be negative");
        if (effectiveFrom == null)
            throw new IllegalArgumentException("effective from is required");
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom))
            throw new IllegalArgumentException("effective to cannot precede effective from");
        // Las dos direcciones, aqui y no en el caso de uso: una linea de flujo sin
        // granularidad no sabe a que fila consumir, y una de existencias con
        // granularidad esta declarando un reinicio que nunca ocurrira.
        if (dimension.measureKind().requiresPeriodKey() && resetPeriod == null)
            throw new IllegalArgumentException("subscription item " + subscriptionItemId
                    + " sells FLOW dimension " + dimension.code() + " without a reset period:"
                    + " seed catalog_item_limits.reset_period for the article, or freeze it in"
                    + " subscription_item_limits, before the contract can grant a ceiling for it");
        if (!dimension.measureKind().requiresPeriodKey() && resetPeriod != null)
            throw new IllegalArgumentException("subscription item " + subscriptionItemId + " sells "
                    + dimension.measureKind() + " dimension " + dimension.code()
                    + " but declares reset period " + resetPeriod + ": only FLOW resets");
    }

    public int ceiling() {
        return includedQuantity + quantity;
    }

    public boolean isCurrentOn(LocalDate day) {
        return !effectiveFrom.isAfter(day) && (effectiveTo == null || effectiveTo.isAfter(day));
    }
}
