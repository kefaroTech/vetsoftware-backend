package com.vetsoftware.app.product.domain;

public class ProductNameAlreadyExistsException extends RuntimeException {
    public ProductNameAlreadyExistsException(String name) {
        super("Ya existe un producto activo con el nombre '" + name + "' en esta empresa.");
    }
}
