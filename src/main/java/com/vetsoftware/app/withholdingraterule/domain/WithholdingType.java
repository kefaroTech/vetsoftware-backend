package com.vetsoftware.app.withholdingraterule.domain;

/**
 * Que impuesto se retiene. Dominio cerrado y <strong>espejo exacto</strong> de
 * {@code chk_withholding_rate_rules_type}: si aqui apareciera un valor que la
 * constraint no admite, el {@code INSERT} lo rechazaria la base y el fallo
 * llegaria como un error de integridad sin explicacion.
 *
 * <p>
 * <strong>{@code ICA} es el unico de los tres que es municipal</strong>, y de
 * ahi sale la mitad de las reglas de esta feature: la tarifa de industria y
 * comercio la fija cada municipio, mientras que retencion en la fuente e IVA
 * son nacionales. Por eso {@code municipalityCode} es obligatorio si y solo si
 * el tipo es {@code ICA} —lo exige tambien
 * {@code chk_withholding_rate_rules_municipality}— y por eso las nacionales
 * comparten el centinela {@code '-'} de la columna generada
 * {@code municipality_key}.
 *
 * <h2>Este nombre simple esta repetido, y NO pasa lo que aqui se decia</h2>
 *
 * <p>
 * Existen tres enums {@code WithholdingType} —este,
 * {@code documentwithholding.domain.WithholdingType} y
 * {@code withholdingcertificate.domain.WithholdingType}— y los tres viajan en
 * un {@code XxxResponse}.
 *
 * <p>
 * <strong>Springdoc funde por nombre simple los <em>records</em>, no los
 * enums.</strong> Un enum no llega a ser un esquema con nombre: springdoc lo
 * <em>inlinea</em> en cada propiedad que lo usa, asi que no hay nada que fundir
 * y los tres conviven sin tocarse. Comprobado contra el contrato real: de los
 * 642 esquemas de {@code api/openapi.json} <strong>ninguno</strong> es un enum,
 * la cadena {@code "WithholdingType"} no aparece <strong>ni una vez</strong> en
 * el fichero, y la lista {@code INCOME_TAX, VAT, ICA} sale repetida inline en
 * las once propiedades que la usan.
 *
 * <p>
 * <strong>Corolario: anadir aqui un cuarto valor NO lo publica en los otros
 * dos.</strong> La version anterior de este javadoc afirmaba lo contrario y
 * mandaba «renombrar el esquema de al menos dos de ellas antes de regenerar
 * {@code api/openapi.json}» — una precaucion imposible de ejecutar, porque esos
 * esquemas no existen, y que a cambio desaconseja un cambio legitimo. Si esta
 * lista tiene que crecer, crece: lo unico que hay que revisar es el
 * {@code CHECK} de <em>esta</em> tabla y los rotulos de los frontends para
 * <em>este</em> endpoint.
 *
 * <p>
 * Lo que si sigue siendo cierto es que la duplicacion es deliberada —el
 * vertical slicing prohibe que un slice importe el dominio de otro— y que hoy
 * los tres declaran exactamente {@code INCOME_TAX}, {@code VAT} e {@code ICA}
 * porque son espejo del mismo {@code CHECK} repetido en los changesets del
 * bloque fiscal.
 */
public enum WithholdingType {

    /** Retencion en la fuente a titulo de renta. Nacional. */
    INCOME_TAX,

    /** Retencion de IVA (reteiva). Nacional. */
    VAT,

    /** Retencion de industria y comercio. La fija el municipio. */
    ICA
}
