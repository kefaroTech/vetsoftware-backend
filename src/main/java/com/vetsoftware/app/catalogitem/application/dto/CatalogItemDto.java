package com.vetsoftware.app.catalogitem.application.dto;

import com.vetsoftware.app.catalogitem.domain.CapacityUnit;
import com.vetsoftware.app.catalogitem.domain.CatalogItem;
import com.vetsoftware.app.catalogitem.domain.CatalogItemStatus;
import com.vetsoftware.app.catalogitem.domain.ItemType;
import java.time.LocalDateTime;

public record CatalogItemDto(Long id, String code, String name, String shortDescription,
        String longDescription, ItemType itemType, CapacityUnit capacityUnit, boolean core,
        int minQuantity, Integer maxQuantity, int sortOrder, CatalogItemStatus status,
        LocalDateTime createdDate, boolean enabled) {

    public static CatalogItemDto from(CatalogItem item) {
        return new CatalogItemDto(item.getId(), item.getCode(), item.getName(),
                item.getShortDescription(), item.getLongDescription(), item.getItemType(),
                item.getCapacityUnit(), item.isCore(), item.getMinQuantity(), item.getMaxQuantity(),
                item.getSortOrder(), item.getStatus(), item.getCreatedDate(), item.isEnabled());
    }
}
