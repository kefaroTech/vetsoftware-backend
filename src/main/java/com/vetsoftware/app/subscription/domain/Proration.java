package com.vetsoftware.app.subscription.domain;

import java.math.BigDecimal;

/**
 * Lo que un otrosi mueve de dinero: el cobro o abono de una sola vez, el cambio
 * de la cuota recurrente, y <strong>la fraccion de dias que los
 * explica</strong>.
 *
 * <p>
 * Los cuatro valores viajan juntos porque separados mienten. Un importe de
 * prorrateo sin su fraccion se ve pero no se puede reconstruir: explicarle a un
 * cliente que reclama de donde salen 34.000 pasa a ser arqueologia —hay que
 * adivinar si se prorrateo sobre 30 dias comerciales o sobre los 31 del mes, y
 * las dos respuestas dan importes distintos que ya nadie puede contrastar—. Es
 * el mismo criterio que impone {@code chk_subscription_charges_proration} sobre
 * el cargo, aqui garantizado por construccion.
 *
 * <p>
 * <strong>Convencion de signos</strong> (§3.1 de
 * {@code suscripciones-modelo.md}): los dos importes van con signo y los dos
 * signos son legitimos. Una alta suma, una baja resta. El signo no se elige:
 * sale de {@code cycleDelta}.
 *
 * @param amount
 *            {@code proration_amount}: lo que se cobra o abona una sola vez por
 *            los dias del periodo en curso afectados por el cambio
 * @param cycleDeltaAmount
 *            {@code monthly_delta_amount}: cuanto sube o baja la factura
 *            recurrente <strong>por ciclo del contrato</strong>. En un contrato
 *            {@code MONTHLY} es literalmente mensual; en uno {@code ANNUAL} es
 *            lo que cambia la factura anual, porque {@code unit_amount} es el
 *            precio congelado del ciclo y dividirlo por doce inventaria un
 *            numero que nadie cobra
 * @param prorationDays
 *            dias del periodo afectados por el cambio
 * @param periodDays
 *            dias naturales del periodo completo. El denominador
 */
public record Proration(BigDecimal amount, BigDecimal cycleDeltaAmount, int prorationDays,
        int periodDays) {

    public Proration {
        if (amount == null)
            throw new IllegalArgumentException("proration amount is required");
        if (cycleDeltaAmount == null)
            throw new IllegalArgumentException("cycle delta amount is required");
        if (periodDays <= 0)
            throw new IllegalArgumentException("periodDays must be greater than zero");
        if (prorationDays < 0)
            throw new IllegalArgumentException("prorationDays cannot be negative");
        if (prorationDays > periodDays)
            throw new IllegalArgumentException("prorationDays cannot exceed periodDays: "
                    + prorationDays + " > " + periodDays);
    }
}
