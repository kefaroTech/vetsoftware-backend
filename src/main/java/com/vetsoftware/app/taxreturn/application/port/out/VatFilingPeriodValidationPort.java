package com.vetsoftware.app.taxreturn.application.port.out;

import com.vetsoftware.app.taxreturn.domain.VatFrequency;

/**
 * La clave foranea compuesta {@code fk_tax_returns_vat_frequency}
 * {@code (vat_frequency_year, vat_frequency) -> vat_filing_periods(fiscal_year,
 * frequency)}, que es de otra feature.
 *
 * <p>
 * <strong>Es lo que convierte la periodicidad del IVA en un dato con vigencia y
 * no en una formula.</strong> La declaracion <em>copia</em> la periodicidad del
 * año y una clave compuesta impide que diverja — el mismo mecanismo que
 * {@code fk_company_capacities_dimension} usa para copiar el
 * {@code measure_kind} del eje. Cuando el impuesto no es IVA, la columna
 * generada {@code vat_frequency_year} vale {@code NULL} e InnoDB no comprueba
 * la clave.
 *
 * <p>
 * Preguntarlo antes evita que un par (año, periodicidad) que no esta publicado
 * salga como violacion de integridad en vez de como «no hay periodicidad de IVA
 * publicada para ese año».
 */
public interface VatFilingPeriodValidationPort {

    /**
     * {@code true} si ese año tiene publicada <b>esa</b> periodicidad de IVA.
     *
     * <p>
     * Recibe el enum de <em>este</em> slice, no el de {@code vatfilingperiod}: el
     * vertical slicing prohibe importar el dominio de otra feature, y la traduccion
     * —por nombre, que es lo unico que las dos listas comparten— vive en el
     * adaptador.
     */
    boolean existsByFiscalYearAndFrequency(int fiscalYear, VatFrequency frequency);
}
