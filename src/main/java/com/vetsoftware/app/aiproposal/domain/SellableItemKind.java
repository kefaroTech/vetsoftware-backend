package com.vetsoftware.app.aiproposal.domain;

/**
 * Tipo de articulo del catalogo comercial, tal como lo necesita esta rodaja.
 *
 * <p>
 * <strong>Enum propio, no el de {@code catalogitem}</strong>: el dominio de una
 * feature nunca importa el de otra. El adaptador que lee el catalogo es el
 * unico fichero que conoce las dos representaciones.
 */
public enum SellableItemKind {

    MODULE,

    CAPACITY,

    BUNDLE,

    ONE_TIME
}
