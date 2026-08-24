package com.vetsoftware.app.quote.domain;

/**
 * Tipo del articulo cotizado, COPIADO del catalogo al congelar la linea
 * (chk_quote_lines_item_type).
 *
 * <p>
 * Es un enum propio de esta feature y no el del catalogo: si manana el catalogo
 * anade un tipo, la cotizacion de ayer sigue diciendo lo que decia.
 */
public enum QuoteItemType {
    MODULE, CAPACITY, ONE_TIME, BUNDLE
}
