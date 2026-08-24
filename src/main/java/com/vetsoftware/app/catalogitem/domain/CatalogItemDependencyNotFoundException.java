package com.vetsoftware.app.catalogitem.domain;

public class CatalogItemDependencyNotFoundException extends RuntimeException {
    public CatalogItemDependencyNotFoundException(Long id) {
        super("CatalogItemDependency not found: " + id);
    }
}
