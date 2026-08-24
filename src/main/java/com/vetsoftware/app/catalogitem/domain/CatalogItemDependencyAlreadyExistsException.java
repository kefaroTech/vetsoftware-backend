package com.vetsoftware.app.catalogitem.domain;

public class CatalogItemDependencyAlreadyExistsException extends RuntimeException {
    public CatalogItemDependencyAlreadyExistsException(Long catalogItemId, Long relatedItemId,
            RelationType relationType) {
        super("Dependency already exists: " + catalogItemId + " " + relationType + " "
                + relatedItemId);
    }
}
