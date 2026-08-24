package com.vetsoftware.app.catalogitem.domain;

public class CatalogItemSubModuleAlreadyExistsException extends RuntimeException {
    public CatalogItemSubModuleAlreadyExistsException(Long catalogItemId, Long subModuleId) {
        super("CatalogItem " + catalogItemId + " already opens sub module " + subModuleId);
    }
}
