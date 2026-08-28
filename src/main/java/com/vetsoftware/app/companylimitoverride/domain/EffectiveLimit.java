package com.vetsoftware.app.companylimitoverride.domain;

/**
 * El techo que rige de verdad, con la etiqueta de de dónde salió.
 *
 * @param limitQuantity
 *            el número, o {@code null} si no hay techo. <strong>Vacío es «sin
 *            techo», que no es lo mismo que cero</strong>: cero es un techo
 *            real que no deja crear nada, y confundirlos es la diferencia entre
 *            una clínica bloqueada y una clínica sin límite.
 * @param overrideId
 *            la excepción negociada de la que salió, cuando el origen es
 *            {@link LimitSource#COMPANY_OVERRIDE}. Es lo que permite abrir el
 *            papel de la decisión desde el contador.
 */
public record EffectiveLimit(Integer limitQuantity, LimitSource source, Long overrideId) {

    public EffectiveLimit {
        if (source == null)
            throw new IllegalArgumentException("limit source is required");
        if (limitQuantity != null && limitQuantity < 0)
            throw new IllegalArgumentException("limit quantity cannot be negative");
        if (source == LimitSource.COMPANY_OVERRIDE && overrideId == null)
            throw new IllegalArgumentException(
                    "a COMPANY_OVERRIDE ceiling must name the override it came from");
        if (source != LimitSource.COMPANY_OVERRIDE && overrideId != null)
            throw new IllegalArgumentException("only a COMPANY_OVERRIDE ceiling names an override");
    }

    /** Sin techo: la clínica crea lo que quiera sobre este eje. */
    public boolean isUnlimited() {
        return limitQuantity == null;
    }

    /**
     * Si crear {@code delta} unidades más dejaría a la empresa por encima del
     * techo. No decide qué hacer al respecto —eso es el modo de aplicación—, solo
     * si se pasa.
     */
    public boolean wouldExceed(int usedQuantity, int delta) {
        return !isUnlimited() && (long) usedQuantity + delta > limitQuantity;
    }
}
