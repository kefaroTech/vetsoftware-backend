package com.vetsoftware.app.platformtaxprofile.domain;

import java.time.LocalDate;

/**
 * La fecha desde la que deberia regir la identidad fiscal nueva no es posterior
 * a la fecha desde la que rige la vigente.
 *
 * <h2>Por que esto no se puede representar</h2>
 *
 * <p>
 * La vigencia es un intervalo <strong>semiabierto</strong>
 * {@code [valid_from, valid_to)}: cerrar la vigente con {@code valid_to = X} y
 * abrir la sucesora con {@code valid_from = X} deja la historia cubierta
 * entera, sin hueco y sin solape. Eso obliga a que {@code X} sea
 * <strong>estrictamente posterior</strong> a la {@code valid_from} de la
 * vigente, y no por gusto: lo exige {@code chk_platform_tax_profiles_validity},
 * que es {@code valid_to > valid_from} —con el {@code >} estricto, no con
 * {@code >=}—, y ademas {@code uq_platform_tax_profiles_validity (valid_from)},
 * que no admite dos identidades empezando el mismo dia porque seria ambiguo
 * cual de las dos rige ese dia.
 *
 * <p>
 * <strong>La consecuencia concreta: una identidad abierta hoy no se puede
 * suceder hoy.</strong> Quien se equivoco al escribir el NIT hace diez minutos
 * no lo arregla con una sucesion fechada hoy; como pronto rige mañana.
 *
 * <h2>Por que se rechaza en vez de correr la fecha</h2>
 *
 * <p>
 * Adelantar la sucesion al dia siguiente por cuenta propia seria escribir en la
 * base que la razon social nueva empieza a regir mañana cuando quien lo pidio
 * dijo hoy. Esa fecha es exactamente lo que decide <strong>que razon social y
 * que NIT se imprimen en una factura emitida en el intervalo</strong>, que es
 * el unico dato que esta feature existe para conservar: moverla en silencio
 * para ahorrarle un rechazo al usuario lo corrompe. Se rechaza, y el mensaje
 * dice desde cuando rige la vigente y cual es la primera fecha posible.
 */
public class PlatformTaxProfileSuccessionNotAfterCurrentException extends RuntimeException {

    private final LocalDate currentValidFrom;
    private final LocalDate requestedEffectiveFrom;

    public PlatformTaxProfileSuccessionNotAfterCurrentException(LocalDate currentValidFrom,
            LocalDate requestedEffectiveFrom) {
        super("The platform tax profile in force since " + currentValidFrom
                + " cannot be succeeded on " + requestedEffectiveFrom
                + ": the earliest possible date is " + currentValidFrom.plusDays(1));
        this.currentValidFrom = currentValidFrom;
        this.requestedEffectiveFrom = requestedEffectiveFrom;
    }

    public LocalDate getCurrentValidFrom() {
        return currentValidFrom;
    }

    public LocalDate getRequestedEffectiveFrom() {
        return requestedEffectiveFrom;
    }

    /** La primera fecha en la que la sucesion si seria representable. */
    public LocalDate getEarliestEffectiveFrom() {
        return currentValidFrom.plusDays(1);
    }
}
