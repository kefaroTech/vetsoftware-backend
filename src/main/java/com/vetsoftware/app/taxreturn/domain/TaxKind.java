package com.vetsoftware.app.taxreturn.domain;

/**
 * Que impuesto se declara. Dominio cerrado y <strong>espejo exacto</strong> de
 * {@code chk_tax_returns_kind} (changeset 351).
 *
 * <p>
 * <strong>Los tres primeros son los mismos literales del tipo de retencion, a
 * proposito.</strong> Lo que se retiene y lo que se declara son el mismo
 * impuesto visto desde dos sitios, y un segundo vocabulario para el mismo
 * concepto es la divergencia silenciosa que el documento maestro persigue.
 *
 * <h2>Cada valor impone una forma distinta de clave de periodo</h2>
 *
 * <p>
 * Es lo que hace {@code chk_tax_returns_period}, y no es burocracia: sin el,
 * una retencion de diciembre acaba declarada en el bimestre de enero y la
 * declaracion se presenta fuera de plazo <b>sin que nada lo delate</b>.
 *
 * <ul>
 * <li>{@link #INCOME_TAX} → {@code 2026-A}, anual.</li>
 * <li>{@link #WITHHOLDING} → {@code 2026-M03}, <b>mensual</b>.</li>
 * <li>{@link #ICA} → {@code 2026-B03}, bimestral.</li>
 * <li>{@link #VAT} → depende de la periodicidad del año, que es un
 * <em>dato</em> de {@code vat_filing_periods} y no una formula.</li>
 * </ul>
 */
public enum TaxKind {

    /** Renta. Anual, y de su firmeza cuelga toda la politica de conservacion. */
    INCOME_TAX,

    /** IVA. Su periodicidad la fija {@code vat_filing_periods} año por año. */
    VAT,

    /** Industria y comercio. Municipal: exige {@code municipalityCode}. */
    ICA,

    /**
     * Retencion en la fuente que <b>nosotros</b> practicamos. Es <b>mensual</b>, al
     * contrario que la que nos practican a nosotros, que se imputa al año gravable
     * de la renta. Misma palabra, dos granularidades legitimas.
     */
    WITHHOLDING
}
