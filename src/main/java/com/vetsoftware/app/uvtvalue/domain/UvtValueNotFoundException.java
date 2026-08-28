package com.vetsoftware.app.uvtvalue.domain;

/**
 * No hay UVT publicada para lo que se pidio. Mapea a 404.
 *
 * <p>
 * El constructor por ano es el que importa: quien calcula una sancion de 2027 y
 * se encuentra con esto tiene que sembrar el ano, no caer en la del ano
 * anterior. Devolver la ultima conocida seria el error silencioso que esta
 * ficha existe para evitar.
 */
public class UvtValueNotFoundException extends RuntimeException {

    public UvtValueNotFoundException(Long id) {
        super("UVT value not found: " + id);
    }

    public UvtValueNotFoundException(int fiscalYear) {
        super("UVT value not published for fiscal year: " + fiscalYear);
    }
}
