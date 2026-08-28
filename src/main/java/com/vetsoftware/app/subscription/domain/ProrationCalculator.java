package com.vetsoftware.app.subscription.domain;

import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;

/**
 * <strong>La aritmetica del prorrateo, escrita una sola vez.</strong>
 *
 * <p>
 * Antes de esta clase los dos importes del otrosi llegaban en el cuerpo de la
 * peticion y se persistian tal cual, es decir: <em>el importe lo dictaba quien
 * mandaba la peticion</em>. Con eso, dos frentes que redondearan distinto
 * grababan importes distintos para la misma operacion, los dos como el otrosi
 * firmado, y nadie lo detectaba —{@code subscription_amendments} es inmutable y
 * no habia formula contra la que contrastarlo—. El principio rector del modelo
 * es el contrario: <em>la cotizacion congela precios y el contrato
 * devenga</em>, asi que la fraccion se deriva del calendario, no de lo que diga
 * el cliente.
 *
 * <h2>La formula</h2>
 *
 * <pre>
 * proration_amount = cycleDelta x prorationDays / periodDays   (2 dec, HALF_UP)
 * </pre>
 *
 * <p>
 * De donde sale cada dato:
 * <ul>
 * <li><strong>{@code cycleDelta}</strong> — lo que cambia la cuota recurrente,
 * calculado por el caso de uso sobre las lineas del contrato:
 * {@link SubscriptionItem#recurringSubtotalOf} = {@code max(cantidad -
 * incluido, 0) x unit_amount}. Es <strong>subtotal, sin impuestos</strong>,
 * igual que {@code subscription_charges.subtotal_amount}: el impuesto lo
 * desglosa el documento, no el cargo. Suma en un alta, resta en una baja, y en
 * un cambio de cantidad es la diferencia entre la linea sucesora y la original.
 * <li><strong>{@code periodDays}</strong> — {@link BillingPeriod#days()} sobre
 * {@code current_period_start/end} del contrato. Dias reales, nunca 30
 * comerciales.
 * <li><strong>{@code prorationDays}</strong> —
 * {@link BillingPeriod#daysCoveredBy}, los dias de este periodo que caen dentro
 * del tramo afectado. El tramo se expresa como {@link EffectivePeriod}, que es
 * la definicion de vigencia del slice: para un alta es el tramo de la propia
 * linea, y para una baja o una cancelacion el tramo abierto desde la fecha
 * efectiva.
 * </ul>
 *
 * <p>
 * <strong>El signo no se decide aqui.</strong> Sale del signo de
 * {@code cycleDelta}, y por eso la misma expresion sirve para las cuatro clases
 * de otrosi: el alta produce un cobro positivo, la baja y la cancelacion un
 * abono negativo, y el cambio de cantidad el que corresponda. Es la §3.1 de
 * {@code suscripciones-modelo.md} —«con signo, una baja resta»— sin ninguna
 * rama que la interprete.
 *
 * <p>
 * <strong>Redondeo</strong>: {@link Money#SCALE} y {@link Money#ROUND}, el
 * mismo del resto del dinero del sistema, aplicado <em>una sola vez</em> al
 * final. {@code HALF_UP} sobre un negativo redondea alejandose del cero, que es
 * lo que hace que un alta y su baja del mismo dia sigan sumando cero.
 *
 * <p>
 * Lo que esta clase <strong>no</strong> hace, y sigue sin estar especificado en
 * el modelo: mora, notas credito y cualquier trato distinto del ciclo
 * {@code ANNUAL} —que aqui se prorratea sobre su propio periodo, como el
 * mensual—.
 */
public final class ProrationCalculator {

    private ProrationCalculator() {
    }

    /**
     * El prorrateo de un cambio, medido contra el periodo de facturacion en curso.
     *
     * @param cycleDelta
     *            cuanto sube (positivo) o baja (negativo) la cuota recurrente del
     *            ciclo. Es el que pone el signo
     * @param affected
     *            el tramo de calendario que el cambio afecta: el de la linea que se
     *            abre, o el abierto desde la fecha efectiva para lo que se cierra
     */
    public static Proration onCurrentPeriod(BigDecimal cycleDelta, BillingPeriod period,
            EffectivePeriod affected) {
        if (cycleDelta == null)
            throw new IllegalArgumentException("cycleDelta is required");
        if (period == null)
            throw new IllegalArgumentException("billing period is required");
        BigDecimal delta = Money.scaled(cycleDelta);
        int periodDays = period.days();
        int prorationDays = period.daysCoveredBy(affected);
        // Cero dias NO es un importe de cero: es que el tramo del cambio no toca el
        // periodo en curso, y eso solo pasa cuando algo esta mal -una fecha efectiva
        // fuera del periodo, o un periodo del contrato que se quedo congelado en el
        // pasado porque nadie lo hacia avanzar-. Devolver Money.zero() aqui, como se
        // hacia antes, guardaba ese fallo como un otrosi firmado, inmutable y de cero
        // pesos, indistinguible de un cambio que legitimamente no movia dinero.
        if (prorationDays == 0)
            throw new ZeroDayProrationException(period.start(), period.end());
        BigDecimal amount = delta.multiply(BigDecimal.valueOf(prorationDays))
                .divide(BigDecimal.valueOf(periodDays), Money.SCALE, Money.ROUND);
        return new Proration(amount, delta, prorationDays, periodDays);
    }
}
