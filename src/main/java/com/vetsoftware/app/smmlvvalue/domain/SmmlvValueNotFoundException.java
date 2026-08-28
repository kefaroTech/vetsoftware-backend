package com.vetsoftware.app.smmlvvalue.domain;

/** No hay salario minimo publicado para lo que se pidio. Mapea a 404. */
public class SmmlvValueNotFoundException extends RuntimeException {

    public SmmlvValueNotFoundException(Long id) {
        super("SMMLV value not found: " + id);
    }

    public SmmlvValueNotFoundException(int fiscalYear) {
        super("SMMLV value not published for fiscal year: " + fiscalYear);
    }
}
