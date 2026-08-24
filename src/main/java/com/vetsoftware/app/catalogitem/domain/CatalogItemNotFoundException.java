package com.vetsoftware.app.catalogitem.domain;

public class CatalogItemNotFoundException extends RuntimeException {
    public CatalogItemNotFoundException(Long id) {
        super("CatalogItem not found: " + id);
    }
}
