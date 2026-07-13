package com.vetsoftware.app.inventory.domain;

/** No hay stock suficiente para una salida y la empresa no permite stock negativo. */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
