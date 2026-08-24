package com.vetsoftware.app.configurator.application.dto;

import com.vetsoftware.app.configurator.domain.SelectedItem;

public record SelectedItemDto(Long catalogItemId, int quantity) {

    public static SelectedItemDto from(SelectedItem item) {
        return new SelectedItemDto(item.catalogItemId(), item.quantity());
    }
}
