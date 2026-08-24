package com.vetsoftware.app.catalogitem.domain;

public class CatalogItemSubModuleNotFoundException extends RuntimeException {
    public CatalogItemSubModuleNotFoundException(Long id) {
        super("CatalogItemSubModule not found: " + id);
    }
}
