package com.vetsoftware.app.accountingexport.domain;

/**
 * Que fichero se le entrega al software contable. Dominio cerrado y
 * <strong>espejo exacto</strong> de {@code chk_accounting_exports_kind}
 * (changeset 345).
 *
 * <p>
 * <strong>Los tres valores son una decision propia y no del documento
 * maestro</strong>, que exigia la columna dentro de la unicidad y nunca dio sus
 * valores (§8.3 de la especificacion). Cambiarlos cuesta un {@code UPDATE} de
 * migracion sobre datos ya sembrados, asi que no se tocan a la ligera.
 */
public enum AccountingExportKind {

    /**
     * El asiento resumen del mes: doce filas al año. Sustituye al diario y a sus
     * renglones, y es lo que hace que la partida doble se pueda comprobar sobre dos
     * numeros ({@code chk_accounting_exports_balanced}).
     */
    JOURNAL_SUMMARY,

    /** El reporte anual de terceros (informacion exogena). */
    THIRD_PARTY_REPORT,

    /** El soporte de la declaracion de IVA del periodo. */
    VAT_SUPPORT
}
