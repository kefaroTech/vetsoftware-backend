package com.vetsoftware.app.pricelist.domain;

public class CatalogPriceNotFoundException extends RuntimeException {
    public CatalogPriceNotFoundException(Long id) {
        super("Catalog price not found: " + id);
    }
}
