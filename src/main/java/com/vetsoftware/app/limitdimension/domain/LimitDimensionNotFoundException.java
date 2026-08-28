package com.vetsoftware.app.limitdimension.domain;

/**
 * El eje pedido no existe. Se distingue de «existe y no tiene fila para esta
 * empresa», que significa techo cero: aquí no hay nada que limitar porque nadie
 * declaró el eje.
 */
public class LimitDimensionNotFoundException extends RuntimeException {

    public LimitDimensionNotFoundException(Long id) {
        super("Limit dimension " + id + " not found");
    }

    public LimitDimensionNotFoundException(String message) {
        super(message);
    }
}
