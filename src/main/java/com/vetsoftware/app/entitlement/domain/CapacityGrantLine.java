package com.vetsoftware.app.entitlement.domain;

import java.time.LocalDate;

/**
 * Linea de capacidad del contrato: el techo contratado de una unidad.
 *
 * <p>
 * El techo de la linea es {@code included_quantity + quantity} porque el
 * configurador ya resto lo incluido antes de fijar la cantidad (R15): lo
 * incluido viene congelado al firmar y lo comprado aparte es {@code quantity}.
 * Sumarlos aqui es lo que hace que "bajar de plan" sea registrable sin destruir
 * nada.
 */
public record CapacityGrantLine(Long subscriptionItemId, CapacityUnit unit, int quantity,
        int includedQuantity, LocalDate effectiveFrom, LocalDate effectiveTo) {

    public CapacityGrantLine {
        if (subscriptionItemId == null)
            throw new IllegalArgumentException("subscription item id is required");
        if (unit == null)
            throw new IllegalArgumentException("capacity unit is required");
        if (quantity <= 0)
            throw new IllegalArgumentException("capacity quantity must be positive");
        if (includedQuantity < 0)
            throw new IllegalArgumentException("included quantity cannot be negative");
        if (effectiveFrom == null)
            throw new IllegalArgumentException("effective from is required");
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom))
            throw new IllegalArgumentException("effective to cannot precede effective from");
    }

    public int ceiling() {
        return includedQuantity + quantity;
    }

    public boolean isCurrentOn(LocalDate day) {
        return !effectiveFrom.isAfter(day) && (effectiveTo == null || effectiveTo.isAfter(day));
    }
}
