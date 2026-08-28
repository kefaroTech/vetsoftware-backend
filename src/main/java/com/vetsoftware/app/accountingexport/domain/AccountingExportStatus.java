package com.vetsoftware.app.accountingexport.domain;

/**
 * En que punto esta la exportacion. Dominio cerrado y <strong>espejo
 * exacto</strong> de {@code chk_accounting_exports_status}, y sus transiciones
 * son las cuatro ramas de {@code chk_accounting_exports_lifecycle}.
 */
public enum AccountingExportStatus {

    /** Generado y aun sin desenlace. Estado inicial, y el defecto de la columna. */
    GENERATED,

    /** Entregado al contador. Exige {@code deliveredAt >= generatedAt}. */
    DELIVERED,

    /**
     * Rechazado. Exige fecha <b>y</b> motivo: un rechazo sin motivo escrito obliga
     * a rehacer el fichero a ciegas.
     */
    REJECTED,

    /**
     * Reemplazado por un intento posterior. Es el unico estado sin condiciones en
     * el {@code CHECK}, porque puede llegar desde cualquiera de los otros tres.
     */
    SUPERSEDED;

    /**
     * {@code true} si la exportacion sigue viva, es decir si ocupa el hueco de
     * {@code uq_accounting_exports_current}.
     *
     * <p>
     * <strong>Esta es la lista que calcula {@code current_export_marker} en la
     * base</strong> ({@code CASE WHEN status IN ('GENERATED','DELIVERED') …}). Si
     * las dos dejaran de coincidir, Java creeria que el hueco esta libre y la base
     * diria que no —o al reves, que es peor: dos ficheros vivos del mismo mes y
     * clase, y el contador sin saber cual vale.
     */
    public boolean occupiesTheCurrentSlot() {
        return this == GENERATED || this == DELIVERED;
    }
}
