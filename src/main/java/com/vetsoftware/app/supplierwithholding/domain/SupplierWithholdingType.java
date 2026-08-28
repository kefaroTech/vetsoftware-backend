package com.vetsoftware.app.supplierwithholding.domain;

/**
 * Que impuesto le retenemos al proveedor. Dominio cerrado y <strong>espejo
 * exacto</strong> de {@code chk_sw_type} (changeset 352), cuyos tres literales
 * son <b>identicos</b> a los de {@code document_withholdings} (329) y
 * {@code withholding_certificates} (328): es el mismo impuesto visto desde la
 * otra direccion.
 *
 * <h2>Por que este enum NO se llama {@code WithholdingType}</h2>
 *
 * <p>
 * En el arbol ya existen tres enums con ese nombre simple
 * —{@code withholdingraterule}, {@code documentwithholding} y
 * {@code withholdingcertificate}—, todos con estos mismos tres valores.
 * <strong>Springdoc funde los esquemas del contrato por nombre simple</strong>:
 * los tres publican uno solo, y hoy eso no miente porque las tres listas son
 * identicas. Añadir una cuarta homonima aumenta la superficie de esa trampa sin
 * ganar nada — el dia que una de las cuatro crezca, el contrato publicaria el
 * valor nuevo tambien para las otras tres, que lo rechazarian desde el binder.
 *
 * <p>
 * El nombre distinto es la escapatoria barata: cuesta una palabra y deja este
 * slice con su propio esquema en {@code api/openapi.json}. El vocabulario que
 * la especificacion exige compartir con {@code document_withholdings} es el de
 * los <em>valores</em> y el de las <em>columnas</em>, no el del nombre de la
 * clase.
 *
 * <h2>Y ojo con {@link #INCOME_TAX}: aqui es MENSUAL</h2>
 *
 * <p>
 * En {@code document_withholdings} —la retencion que <b>nos practican</b>— el
 * periodo es anual ({@code 2026-A}), porque se imputa al año gravable de
 * nuestra renta. La que <b>nosotros practicamos</b> se declara en la retencion
 * en la fuente, que es mensual ({@code 2026-M03}). Misma columna, mismo nombre,
 * dos granularidades legitimas — y {@code chk_sw_period} lo impone.
 */
public enum SupplierWithholdingType {

    /** Retencion en la fuente a titulo de renta. Nacional y <b>mensual</b>. */
    INCOME_TAX,

    /** Retencion de IVA (reteiva). Nacional y bimestral. */
    VAT,

    /** Retencion de industria y comercio. La fija el municipio; bimestral. */
    ICA
}
