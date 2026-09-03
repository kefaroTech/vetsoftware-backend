package com.vetsoftware.app.pricelist.application.dto;

public record PublicCatalogAreaDto(String code, String name) {

    public static PublicCatalogAreaDto from(PublicCatalogAreaRowDto row) {
        return new PublicCatalogAreaDto(row.code(), row.name());
    }
}
