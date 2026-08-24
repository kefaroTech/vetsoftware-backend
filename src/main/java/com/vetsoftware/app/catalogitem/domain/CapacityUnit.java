package com.vetsoftware.app.catalogitem.domain;

/**
 * Unidad de un artículo de tipo {@link ItemType#CAPACITY}.
 *
 * <p>
 * Espejo de {@code chk_catalog_items_capacity_unit}, que hace dos trabajos en
 * una sola constraint: cierra el dominio y ata la unidad al tipo. Sin esa
 * atadura se puede vender un {@code MODULE} con unidad {@code USER}, que el
 * configurador interpretaría como un contador y sumaría usuarios que nadie
 * compró.
 */
public enum CapacityUnit {
    USER, BRANCH, TERMINAL, STORAGE_GB
}
