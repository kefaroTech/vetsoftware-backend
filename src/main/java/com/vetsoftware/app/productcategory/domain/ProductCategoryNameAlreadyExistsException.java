package com.vetsoftware.app.productcategory.domain;

public class ProductCategoryNameAlreadyExistsException extends RuntimeException {
    public ProductCategoryNameAlreadyExistsException(String name) {
        super("Ya existe una categoría de producto activa con el nombre '" + name + "' en esta empresa.");
    }
}
