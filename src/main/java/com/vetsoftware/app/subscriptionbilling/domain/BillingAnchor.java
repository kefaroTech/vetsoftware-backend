package com.vetsoftware.app.subscriptionbilling.domain;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * El día del mes al que queda anclado el reloj de cobro de un contrato.
 *
 * <p>
 * <b>El ancla no se degrada, y ese es el único motivo por el que este tipo
 * existe.</b> La forma ingenua de avanzar un periodo mensual es
 * {@code currentPeriodStart.plusMonths(1)}, y con eso un contrato anclado al 31
 * factura el 28 de febrero y a partir de ahí <em>se queda</em> en el 28: marzo,
 * abril, mayo y todos los meses siguientes. El cliente pierde tres días de
 * servicio cada año y nadie lo nota, porque cada paso individual es correcto —
 * lo que está mal es que el paso lea el resultado del paso anterior en vez de
 * leer el ancla.
 *
 * <p>
 * Aquí el ancla es un número inmutable que se guarda una vez y se aplica a cada
 * mes por separado: {@code 31} sobre febrero da el 28, y el mes siguiente
 * vuelve a preguntarle al ancla y da el 31. La degradación es imposible por
 * construcción, no por disciplina.
 *
 * <p>
 * <b>De dónde sale el número</b>, y por qué no de la firma: lo fija
 * {@link BillingCycleSubscription#firstBillableStart()}, que es el día
 * siguiente al fin de la prueba. Quien firma el 31 de enero con treinta días de
 * prueba empieza a devengar el 2 de marzo, así que su ancla es el 2 y no el 31.
 */
public record BillingAnchor(int dayOfMonth) {

    /** Ningún mes tiene más de 31 días; el ancla admite el máximo. */
    public static final int MAX_DAY = 31;

    public BillingAnchor {
        if (dayOfMonth < 1 || dayOfMonth > MAX_DAY)
            throw new IllegalArgumentException(
                    "billing anchor day must be between 1 and " + MAX_DAY + ", got " + dayOfMonth);
    }

    /**
     * El ancla que corresponde a la fecha en que el contrato empieza a devengar.
     *
     * <p>
     * <b>Se construye siempre desde una fecha inmutable</b> —el fin de la prueba o
     * la fecha de inicio—, nunca desde {@code current_period_start}, que sí se
     * mueve: derivarla de un campo que el propio proceso reescribe es exactamente
     * cómo se degrada un ancla.
     */
    public static BillingAnchor from(LocalDate anchorDate) {
        if (anchorDate == null)
            throw new IllegalArgumentException("anchor date is required");
        return new BillingAnchor(anchorDate.getDayOfMonth());
    }

    /**
     * El ancla materializada sobre un mes concreto, recortada a su último día
     * cuando el mes es más corto.
     *
     * <p>
     * El recorte es la <em>presentación</em> del ancla en ese mes y no una
     * modificación del ancla: quien pregunte por el mes siguiente vuelve a recibir
     * el 31.
     */
    public LocalDate onMonth(YearMonth month) {
        if (month == null)
            throw new IllegalArgumentException("month is required");
        return month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));
    }
}
