package com.vetsoftware.app.quote.domain;

/**
 * Companion VO del articulo del catalogo, leido UNA SOLA VEZ: en el momento de
 * congelar la linea.
 *
 * <p>
 * Despues de eso la cotizacion vive de las copias de {@link QuoteLine} y este
 * VO no vuelve a aparecer. Si algun dia alguien lo usa para resolver el nombre
 * o el importe al pintar una cotizacion ya guardada, ha roto el modelo.
 */
public record CatalogItemRef(Long id, String code, String name, QuoteItemType itemType) {
    public CatalogItemRef {
        if (id == null)
            throw new IllegalArgumentException("catalog item id is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("catalog item code is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("catalog item name is required");
        if (itemType == null)
            throw new IllegalArgumentException("catalog item type is required");
    }
}
