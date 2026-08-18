package com.vetsoftware.app.salesreport.domain;

import java.time.LocalDate;

/**
 * Periodo de un reporte fiscal de ventas, con su invariante en el constructor.
 *
 * <p>
 * {@code salesreport} no tiene entidad de dominio —sus dos casos de uso son
 * consultas de agregacion sobre documentos de otra feature—, asi que la regla
 * «las validaciones de negocio van en el constructor, no en el controller ni en
 * el service» se cumple aqui: un value object propio de la feature que ningun
 * reporte puede construir mal.
 *
 * <p>
 * Por que importa que un rango invertido falle en vez de devolver ceros: sin
 * esta invariante, {@code from > to} no filtraba ningun documento y producia un
 * libro de ventas o una conciliacion DIAN formalmente validos, con todo en
 * cero, <b>indistinguibles de un periodo real sin ventas</b>. Un reporte fiscal
 * que miente en silencio es peor que uno que falla.
 */
public record ReportDateRange(LocalDate from, LocalDate to) {

    public ReportDateRange {
        if (from == null)
            throw new IllegalArgumentException("'from' is required");
        if (to == null)
            throw new IllegalArgumentException("'to' is required");
        if (from.isAfter(to))
            throw new IllegalArgumentException(
                    "'from' must not be after 'to': from=" + from + ", to=" + to);
    }
}
