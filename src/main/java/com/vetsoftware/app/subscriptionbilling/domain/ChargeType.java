package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * Qué clase de devengo representa un cargo.
 *
 * <p>
 * Espejo de {@code chk_subscription_charges_type}, y la mitad de
 * {@code chk_subscription_charges_sign}: el tipo decide qué signos admite el
 * {@code subtotal_amount} del cargo.
 */
public enum ChargeType {
    /** La cuota del ciclo. Siempre suma: {@code subtotal_amount >= 0}. */
    RECURRING,
    /**
     * El proporcional de un cambio a mitad de ciclo. <b>Libre de signo a
     * propósito</b>: una ampliación cobra (positivo) y una reducción acredita
     * (negativo), y las dos son operaciones normales.
     */
    PRORATION,
    /** Implantación, migración, capacitación. Siempre suma. */
    ONE_TIME,
    /** Lo que se le devuelve al cliente. Siempre resta. */
    CREDIT,
    /** Descuento comercial. Siempre resta. */
    DISCOUNT,
    /**
     * <b>El consumo por encima del cupo contratado</b>, cuando la línea del
     * contrato declaró {@code enforcement = OVERAGE} y su precio por unidad.
     * Siempre suma: es servicio prestado de más, no una devolución.
     *
     * <p>
     * <b>Existe porque su ausencia bloqueaba a quien paga.</b> Sin esta clase de
     * cargo no había forma de cobrar el excedente, así que
     * {@code AdjustCompanyCapacityUsageService} negaba el consumo
     * <em>incondicionalmente</em> al pasar del techo — también a la clínica cuya
     * suscripción declaraba modo de excedente y precio por unidad, es decir a la
     * que el modelo dice que <b>debe</b> poder pasarse y está dispuesta a pagarlo.
     * El modo estaba declarado en {@code subscription_item_limits.enforcement}
     * desde el changeset 304 y no tenía a dónde ir.
     *
     * <p>
     * <b>Tres cosas que hay que saber antes de tocarlo:</b>
     *
     * <ol>
     * <li><b>El esquema lo admite desde el changeset 374, y hubo que ampliar DOS
     * restricciones, no una.</b> {@code chk_subscription_charges_type} (changeset
     * 251) enumeraba cinco literales, pero {@code chk_subscription_charges_sign}
     * <b>también</b> los enumera uno a uno: ampliar solo la primera habría dejado
     * la fila muriendo igual en el motor, y con un error que señala a la
     * restricción equivocada. El excedente entró en la rama de signo positivo,
     * junto a {@code RECURRING} y {@code ONE_TIME}, que es lo que
     * {@link #exigeSubtotalNoNegativo()} ya declaraba en código.
     * <li><b>Queda fuera de {@code uq_subscription_charges_recurring}</b>
     * (changeset 372) <b>por diseño</b>: {@code recurring_charge_key} solo se
     * rellena cuando {@code charge_type = 'RECURRING'} y vale {@code NULL} en
     * cualquier otro caso. Es lo correcto —un mismo contrato puede excederse varias
     * veces en el mismo periodo y cada exceso es un cargo legítimo, así que una
     * unicidad por línea y periodo haría inescribible el segundo—, pero significa
     * que <b>el reintento de un excedente no choca contra nada</b>. La llave
     * antiduplicados del excedente no es esta tabla sino {@code uq_cue_fact} sobre
     * {@code company_usage_events}, y depende de que {@code occurred_at} sea el
     * instante del registro consumido y no el del reloj del proceso.
     * <li><b>No cabe sobre un eje acumulativo</b>
     * ({@code chk_subscription_item_limits_overage}: {@code measure_kind <>
     * 'CUMULATIVE'}). Lo impone la base y lo repite {@code SubscriptionItemLimit};
     * aquí no se vuelve a comprobar porque este enum no conoce el eje.
     * </ol>
     */
    OVERAGE;

    /** {@code true} si el tipo obliga a un subtotal positivo o cero. */
    public boolean exigeSubtotalNoNegativo() {
        return this == RECURRING || this == ONE_TIME || this == OVERAGE;
    }

    /** {@code true} si el tipo obliga a un subtotal negativo o cero. */
    public boolean exigeSubtotalNoPositivo() {
        return this == CREDIT || this == DISCOUNT;
    }
}
