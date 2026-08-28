package com.vetsoftware.app.entitlement.domain;

import java.time.LocalDate;

/**
 * Una linea del contrato proyectada sobre un submodulo concreto: "este cliente
 * tiene contratado esto, desde esta fecha y hasta esta otra".
 *
 * <p>
 * {@code readOnlyCapable} viaja aqui y no en {@link SubModuleRef} a proposito:
 * es un dato del catalogo que solo hace falta <em>mientras se calcula</em>, no
 * algo que el permiso guarde. Lo mismo vale para {@code degradationImmune}.
 *
 * <p>
 * <strong>El modo de cobro y el fin de prueba son de la linea, no del
 * contrato</strong> (R-TRIAL-13, R-TRIAL-15). Un mismo contrato lleva a la vez
 * lineas en prueba y lineas de pago obligatorio, y cada una vence por su
 * cuenta: mirar {@code subscriptions.status} para decidir si algo esta en
 * prueba es la trampa que D-01 obliga a desactivar, y hace que un solo dia de
 * mora mate la prueba para siempre.
 *
 * <p>
 * {@code trialOutcome} es el desenlace <strong>congelado el dia que se
 * concedio</strong> la prueba, no la politica viva del catalogo: cambiar de 30
 * a 14 los dias de historia clinica no le cambia nada a quien ya esta probando
 * (R-TRIAL-28).
 *
 * @param chargeMode
 *            como cobra esta linea. Nunca nulo: una linea sin modo se trata
 *            como {@link LineChargeMode#PAID}, que es el defecto de la columna.
 * @param trialEndDate
 *            ultimo dia de prueba, <strong>inclusive</strong>. Obligatorio si
 *            el modo es {@code TRIAL} y prohibido si no lo es (R-TRIAL-07).
 * @param trialOutcome
 *            que pasa el dia siguiente al vencimiento. Obligatorio si el modo
 *            es {@code TRIAL}: sin el no se puede escribir la fila sucesora y
 *            el acceso desapareceria en vez de bajar.
 * @param degradationImmune
 *            el submodulo no se degrada jamas, ni por mora, ni por cupo, ni por
 *            baja (R-ENT-05). Es la unica barandilla entre una discusion
 *            comercial y que una clinica no pueda emitir sus facturas.
 */
public record ModuleGrantLine(Long subscriptionItemId, SubModuleRef subModule,
        boolean readOnlyCapable, LocalDate effectiveFrom, LocalDate effectiveTo, boolean core,
        LineChargeMode chargeMode, LocalDate trialEndDate, TrialOutcomePolicy trialOutcome,
        boolean degradationImmune) {

    public ModuleGrantLine {
        if (subscriptionItemId == null)
            throw new IllegalArgumentException("subscription item id is required");
        if (subModule == null)
            throw new IllegalArgumentException("sub module is required");
        if (effectiveFrom == null)
            throw new IllegalArgumentException("effective from is required");
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom))
            throw new IllegalArgumentException("effective to cannot precede effective from");
        if (chargeMode == null)
            chargeMode = LineChargeMode.PAID;
        // Espejo de chk_subscription_items_trial (R-TRIAL-07): una linea TRIAL sin
        // fecha de caducidad seria una prueba eterna, y una linea PAID con fecha de
        // prueba es una prueba que nadie sabe que existe.
        if (chargeMode.isTrial() && trialEndDate == null)
            throw new IllegalArgumentException("trial end date is required for a TRIAL line: item "
                    + subscriptionItemId + " would be an endless trial");
        if (!chargeMode.isTrial() && trialEndDate != null)
            throw new IllegalArgumentException("trial end date is only allowed on a TRIAL line,"
                    + " and item " + subscriptionItemId + " is " + chargeMode);
        // Sin desenlace congelado no hay fila sucesora que escribir, y sin fila
        // sucesora el acceso DESAPARECE al vencer en vez de bajar (R-ENT-01).
        if (chargeMode.isTrial() && trialOutcome == null)
            throw new IllegalArgumentException("trial outcome is required for a TRIAL line: item "
                    + subscriptionItemId + " has no frozen outcome, so its access would vanish"
                    + " on expiry instead of stepping down");
    }

    /**
     * Linea que no esta en prueba y no es inmune a la degradacion, que es el caso
     * de la inmensa mayoria del contrato. Existe para que los llamadores que no
     * saben nada de la capa de prueba no tengan que inventarse cuatro valores.
     */
    public ModuleGrantLine(Long subscriptionItemId, SubModuleRef subModule, boolean readOnlyCapable,
            LocalDate effectiveFrom, LocalDate effectiveTo, boolean core) {
        this(subscriptionItemId, subModule, readOnlyCapable, effectiveFrom, effectiveTo, core,
                LineChargeMode.PAID, null, null, false);
    }

    /**
     * Vigente el dia indicado: ya empezo y todavia no ha terminado.
     * {@code effective_to} es <strong>exclusiva</strong>, igual que en la consulta
     * de vigencia de {@code subscription_items}.
     */
    public boolean isCurrentOn(LocalDate day) {
        return !effectiveFrom.isAfter(day) && (effectiveTo == null || effectiveTo.isAfter(day));
    }

    /** Ya termino: la baja del modulo ocurrio y no es una linea futura. */
    public boolean hasEndedOn(LocalDate day) {
        return effectiveTo != null && !effectiveTo.isAfter(day);
    }

    /**
     * El instante en que se cierra la ventana de prueba de <strong>esta</strong>
     * linea, o {@code null} si no esta en prueba.
     *
     * <p>
     * {@code trial_end_date} es el ultimo dia de prueba, inclusive: la ventana se
     * cierra al arrancar el dia siguiente. Sin el {@code plusDays(1)} la prueba
     * moriria un dia antes, y el ultimo dia --que el cliente ya pago con su
     * atencion-- no existiria.
     */
    public java.time.LocalDateTime trialClosesAt() {
        return trialEndDate == null ? null : trialEndDate.plusDays(1).atStartOfDay();
    }
}
