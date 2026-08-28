package com.vetsoftware.app.taxreturn.domain;

/**
 * Cada cuanto se declara el IVA. Dominio cerrado y <strong>espejo
 * exacto</strong> de {@code chk_tax_returns_vat_freq}.
 *
 * <p>
 * <strong>El valor se COPIA de {@code vat_filing_periods} y una clave foranea
 * compuesta impide que diverja.</strong> {@code fk_tax_returns_vat_frequency}
 * apunta a {@code (fiscal_year, frequency)} desde la columna generada
 * {@code vat_frequency_year} y esta: cuando el impuesto no es IVA la generada
 * vale {@code NULL} e InnoDB no comprueba la clave. Es el mismo mecanismo con
 * el que {@code fk_company_capacities_dimension} copia el {@code measure_kind}
 * del eje, y lo que convierte la periodicidad en un <em>dato con vigencia</em>
 * en vez de una formula escrita en el codigo.
 *
 * <h2>Este nombre simple es DISTINTO del de {@code vatfilingperiod} a
 * proposito</h2>
 *
 * <p>
 * Alli el enum se llama {@code VatFilingFrequency}. Los dos declaran los mismos
 * tres valores, pero <strong>springdoc funde los esquemas del contrato por
 * nombre simple</strong>: si los dos se llamaran igual, los dos frontends
 * recibirian uno solo y el dia que uno de los dos creciera —una periodicidad
 * nueva— el otro publicaria un valor que su endpoint rechaza. Con nombres
 * distintos, cada uno viaja con su propia lista.
 *
 * <p>
 * Art. 600 ET: bimestral para responsables nuevos y para ingresos brutos del
 * año anterior iguales o superiores a 92.000 UVT; cuatrimestral por debajo.
 */
public enum VatFrequency {

    /** Bimestral: seis periodos al año, {@code 2026-B01} a {@code 2026-B06}. */
    BIMONTHLY,

    /** Cuatrimestral: tres periodos, {@code 2026-C01} a {@code 2026-C03}. */
    FOURMONTHLY,

    /** Anual: {@code 2026-A}. */
    ANNUAL
}
