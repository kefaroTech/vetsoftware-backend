package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * Cada cuánto vuelve a devengarse la cuota.
 *
 * <p>
 * <b>Companion VO, no un import.</b> Espeja {@code subscriptions.billing_cycle}
 * ({@code VARCHAR(20)}), cuyo enum vive en {@code pricelist} y que el vertical
 * slicing prohíbe traer aquí. El valor se traduce en el adaptador y entra al
 * dominio como uno de estos dos.
 */
public enum BillingPeriodicity {

    MONTHLY(1), ANNUAL(12);

    private final int months;

    BillingPeriodicity(int months) {
        this.months = months;
    }

    /** Meses que dura un periodo de esta periodicidad. */
    public int months() {
        return months;
    }

    /**
     * Traduce el texto crudo de la columna.
     *
     * <p>
     * <b>Un valor desconocido revienta.</b> Degradarlo a {@code MONTHLY} le
     * cobraría doce veces al año a quien contrató una anual, y degradarlo a «no
     * factura» dejaría de cobrarle en silencio. Las dos son peores que parar el
     * barrido con un mensaje que nombra el valor.
     */
    public static BillingPeriodicity de(String value) {
        if (value == null || value.isBlank())
            throw new IllegalStateException("subscriptions.billing_cycle is empty:"
                    + " billing cannot decide how long the period lasts");
        try {
            return valueOf(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Unknown subscriptions.billing_cycle '" + value
                    + "': billing cannot decide how long the period lasts", exception);
        }
    }
}
