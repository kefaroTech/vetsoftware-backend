package com.vetsoftware.app.vatfilingperiod.domain;

/**
 * Cada cuanto se declara IVA. Espejo <strong>literal</strong> de
 * {@code chk_vat_filing_periods_frequency}: los tres nombres se escriben aqui
 * igual que en la constraint porque {@code @Enumerated(EnumType.STRING)} guarda
 * el {@code name()} tal cual.
 *
 * <p>
 * <strong>Es un dato con vigencia por ano, no una formula.</strong> El primer
 * ano de un responsable nuevo es bimestral por ley (art. 600 num. 1 ET) —no hay
 * ingresos del ano anterior con los que decidir— y despues puede cambiar segun
 * lo que se facturara. Deducirlo en tiempo de ejecucion a partir de los
 * ingresos daria una periodicidad distinta cada vez que se recalcula el pasado.
 */
public enum VatFilingFrequency {

    /** Bimestral. */
    BIMONTHLY,

    /** Cuatrimestral. */
    FOURMONTHLY,

    /** Anual. */
    ANNUAL
}
