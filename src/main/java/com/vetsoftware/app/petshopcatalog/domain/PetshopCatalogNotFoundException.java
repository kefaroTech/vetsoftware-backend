package com.vetsoftware.app.petshopcatalog.domain;

public class PetshopCatalogNotFoundException extends RuntimeException {
    public PetshopCatalogNotFoundException(String resource, Object id) {
        super(resource + " not found: " + id);
    }
}
