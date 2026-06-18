package com.vetsoftware.app.product.domain;

public class ProductCodeAlreadyExistsException extends RuntimeException {
    public ProductCodeAlreadyExistsException(String code) {
        super("Ya existe un producto activo con el código '" + code + "' en esta empresa.");
    }
}
