package com.vetsoftware.app.limitdimension.domain;

/**
 * Dos ejes con el mismo código serían dos respuestas válidas a «¿cuántas
 * mascotas puede crear?». Lo impide {@code uq_limit_dimensions_code}; esta
 * excepción es la traducción legible de ese choque.
 */
public class LimitDimensionCodeAlreadyExistsException extends RuntimeException {

    public LimitDimensionCodeAlreadyExistsException(String code) {
        super("Limit dimension code already exists: " + code);
    }
}
