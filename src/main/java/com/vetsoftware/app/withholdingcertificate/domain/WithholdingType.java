package com.vetsoftware.app.withholdingcertificate.domain;

/**
 * Que impuesto retuvo el cliente. Lista cerrada y espejo exacto de
 * {@code chk_withholding_certificates_type}: si aqui aparece un valor que la
 * constraint no admite, el {@code INSERT} lo rechaza la base y el fallo llega
 * como un error de integridad sin explicacion.
 *
 * <p>
 * <strong>Es copia local de esta feature y no un tipo compartido.</strong> El
 * vertical slicing lo exige —nada se comparte entre paquetes raiz— y ademas la
 * lista no significa lo mismo en todas partes: aqui son los tres impuestos que
 * un certificado puede acreditar, y el periodo fiscal valido depende de cual
 * sea ({@code INCOME_TAX} es anual, {@code VAT} e {@code ICA} bimestrales).
 */
public enum WithholdingType {

    /** Retencion en la fuente sobre renta. Se certifica por ano gravable. */
    INCOME_TAX,

    /** Retencion de IVA. Se certifica por bimestre. */
    VAT,

    /** Retencion de industria y comercio. Se certifica por bimestre. */
    ICA
}
