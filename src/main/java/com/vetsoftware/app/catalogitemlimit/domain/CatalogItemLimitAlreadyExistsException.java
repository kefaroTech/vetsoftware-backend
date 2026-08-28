package com.vetsoftware.app.catalogitemlimit.domain;

/**
 * Ya hay un techo de fábrica para ese artículo y ese eje. Dos serían dos
 * respuestas válidas a la misma pregunta; lo impone
 * {@code uq_catalog_item_limits}.
 */
public class CatalogItemLimitAlreadyExistsException extends RuntimeException {

    public CatalogItemLimitAlreadyExistsException(Long catalogItemId, Long limitDimensionId) {
        super("Catalog item " + catalogItemId + " already declares a factory limit for dimension "
                + limitDimensionId);
    }
}
