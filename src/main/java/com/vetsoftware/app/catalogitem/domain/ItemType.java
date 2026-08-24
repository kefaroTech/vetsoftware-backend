package com.vetsoftware.app.catalogitem.domain;

/**
 * Qué clase de cosa es un artículo del catálogo comercial.
 *
 * <p>
 * Espejo de {@code chk_catalog_items_item_type}. Los códigos van en inglés como
 * los 105 tipos cerrados del árbol: el documento de diseño escribía en español
 * la terna de las dependencias y la especificación resolvió el choque C2 a
 * favor del inglés.
 */
public enum ItemType {
    /** Funcionalidad que se enciende: abre uno o varios submódulos. */
    MODULE,
    /** Contador que se compra por unidades: usuarios, sedes, terminales. */
    CAPACITY,
    /** Cobro único: implantación, migración, capacitación. */
    ONE_TIME,
    /** Paquete que agrupa otros artículos. */
    BUNDLE
}
