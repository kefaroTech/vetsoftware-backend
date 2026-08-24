package com.vetsoftware.app.catalogitem.domain;

/**
 * El codigo de un articulo es unico en toda la plataforma
 * ({@code uq_catalog_items_code}) y ademas inmutable, asi que chocar con uno
 * existente solo puede pasar al crear.
 *
 * <p>
 * Se comprueba antes de insertar en vez de dejar que salte la constraint: el
 * duplicado tiene que llegar como un 409 que nombra el codigo, no como el 500
 * en que se traduce una violacion de integridad.
 */
public class CatalogItemCodeAlreadyExistsException extends RuntimeException {
    public CatalogItemCodeAlreadyExistsException(String code) {
        super("CatalogItem code already exists: " + code);
    }
}
