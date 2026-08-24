package com.vetsoftware.app.catalogitem.domain;

/**
 * Ciclo de vida comercial de un artículo. Espejo de
 * {@code chk_catalog_items_status}.
 */
public enum CatalogItemStatus {
    /** En preparación: no se ofrece al cliente. */
    DRAFT,
    /** Vendible: es lo que lista el configurador. */
    ACTIVE,
    /** Retirado de la venta; sigue vivo en los contratos que ya lo tienen. */
    DEPRECATED
}
