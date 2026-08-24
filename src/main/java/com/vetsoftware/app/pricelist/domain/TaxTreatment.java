package com.vetsoftware.app.pricelist.domain;

/**
 * Tratamiento fiscal de un precio.
 *
 * <p>
 * <strong>{@link #EXEMPT} y {@link #EXCLUDED} no se pueden colapsar en «tarifa
 * cero».</strong> Excluido y exento se declaran distinto y dan derechos
 * distintos ante la DIAN; confundirlos es un error que solo aparece en una
 * revisión fiscal. Los dos llevan {@code taxRate = 0}, y esa igualdad numérica
 * es justamente lo que hace imprescindible conservar el código: sin él,
 * {@code taxRate = 0} es ambiguo entre exento, excluido y gravado al 0 %.
 *
 * <p>
 * El impuesto vive en el precio y no en el artículo porque el catálogo de
 * impuestos del árbol es por clínica, y un artículo global de plataforma no
 * puede apuntar ahí.
 */
public enum TaxTreatment {
    /** Gravado: exige una tarifa mayor que cero. */
    TAXED,
    /** Exento: tarifa cero, con derecho a descontar el IVA de los insumos. */
    EXEMPT,
    /** Excluido: tarifa cero, sin derecho a descuento. */
    EXCLUDED
}
