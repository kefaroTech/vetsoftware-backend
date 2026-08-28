package com.vetsoftware.app.catalogitemlimit.domain;

/** No hay techo de fábrica declarado para ese artículo y ese eje. */
public class CatalogItemLimitNotFoundException extends RuntimeException {

    public CatalogItemLimitNotFoundException(Long id) {
        super("Catalog item limit " + id + " not found");
    }

    public CatalogItemLimitNotFoundException(Long catalogItemId, Long limitDimensionId) {
        super("Catalog item " + catalogItemId + " has no factory limit for dimension "
                + limitDimensionId);
    }
}
