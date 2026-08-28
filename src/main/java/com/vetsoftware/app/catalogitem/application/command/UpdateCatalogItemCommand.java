package com.vetsoftware.app.catalogitem.application.command;

import com.vetsoftware.app.catalogitem.domain.CatalogItemStatus;
import com.vetsoftware.app.catalogitem.domain.ItemType;

/**
 * Sin {@code code}: el codigo de un articulo es inmutable por ficha, porque es
 * lo que copian congelado las lineas de cotizacion y de contrato.
 */
public record UpdateCatalogItemCommand(Long id, String name, String shortDescription,
        String longDescription, ItemType itemType, String capacityUnit, boolean core,
        int minQuantity, Integer maxQuantity, int sortOrder, CatalogItemStatus status) {
}
