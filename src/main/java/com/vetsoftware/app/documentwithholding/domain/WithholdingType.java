package com.vetsoftware.app.documentwithholding.domain;

/**
 * Los tres impuestos que un cliente puede retenerte al pagarte. Espejo exacto
 * de {@code chk_document_withholdings_type}: un valor que aqui exista y la
 * constraint no admita se rechaza en la base, y el fallo llega como un error de
 * integridad sin explicacion.
 *
 * <p>
 * <strong>Los literales son {@code INCOME_TAX}, {@code VAT} e {@code ICA}, no
 * {@code RENTA} ni {@code RETEIVA}.</strong> No es una preferencia de idioma:
 * son los valores que el {@code CHECK} enumera, y el {@code CHECK} es quien
 * manda. Renombrar uno aqui sin tocar la migracion deja la feature escribiendo
 * filas que la base rechaza una a una.
 *
 * <p>
 * <strong>Copia local de la feature, y el vertical slicing lo exige.</strong>
 * {@code withholding_certificates} y {@code withholding_rate_rules} tienen su
 * propia lista con los mismos tres valores. Compartir un enum entre rodajas
 * ataria las tres a la forma de una, y springdoc ademas funde los esquemas por
 * nombre simple: si otra feature publica un enum homonimo con otros valores, el
 * contrato saldria con la union de los dos.
 *
 * <p>
 * <strong>La granularidad fiscal de cada uno es distinta y por eso importa a
 * quien lea esto:</strong> la renta se declara una vez al ano, mientras que el
 * IVA y el ICA se imputan <em>por bimestre</em>. Es lo que decide la forma de
 * {@code fiscalPeriodKey} en {@link DocumentWithholding}.
 */
public enum WithholdingType {

    /**
     * Retencion en la fuente a titulo de renta. Anual: el periodo es
     * {@code YYYY-A}.
     */
    INCOME_TAX,

    /**
     * Retencion de IVA. Bimestral: el periodo es
     * {@code YYYY-B01}..{@code YYYY-B06}.
     */
    VAT,

    /**
     * Retencion de industria y comercio. Bimestral <strong>y municipal</strong>:
     * exige {@code municipalityCode} porque la tarifa cambia de municipio a
     * municipio.
     */
    ICA
}
