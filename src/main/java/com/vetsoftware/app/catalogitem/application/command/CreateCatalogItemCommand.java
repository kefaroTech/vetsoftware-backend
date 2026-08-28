package com.vetsoftware.app.catalogitem.application.command;

import com.vetsoftware.app.catalogitem.domain.CatalogItemStatus;
import com.vetsoftware.app.catalogitem.domain.ItemType;

public record CreateCatalogItemCommand(String code, String name, String shortDescription,
        String longDescription, ItemType itemType, String capacityUnit, boolean core,
        int minQuantity, Integer maxQuantity, int sortOrder, CatalogItemStatus status) {
}
