package com.vetsoftware.app.pricelist.application.dto;

import com.vetsoftware.app.pricelist.domain.CatalogItemRef;

/** El articulo tal como lo necesita quien pinta una fila de precio. */
public record CatalogItemSummaryDto(Long id, String code, String name) {

    public static CatalogItemSummaryDto from(CatalogItemRef ref) {
        return ref == null ? null : new CatalogItemSummaryDto(ref.id(), ref.code(), ref.name());
    }
}
