package com.vetsoftware.app.pricelist.domain;

/**
 * Companion VO del articulo al que esta feature le pone precio.
 *
 * <p>
 * Existe porque el listado de precios de una tarifa tenia que devolver
 * {@code catalogItemId} y nada mas, y la consola no puede pintar el nombre de
 * un articulo que no viene: o se traia el catalogo entero y cruzaba en cliente
 * -que falla en silencio en cuanto el catalogo se pagine de verdad- o hacia una
 * peticion por fila. Incidencia #379.
 *
 * <p>
 * <strong>Es de lectura y solo de lectura.</strong> Ninguna invariante de
 * {@code pricelist} depende de estos campos: el precio se sigue identificando
 * por {@code (lista, articulo, ciclo, tramo)}. Por eso convive con
 * {@link com.vetsoftware.app.pricelist.application.port.out.CatalogItemValidationPort},
 * que es la guarda de existencia del camino de escritura y no necesita traer
 * datos. Si algun dia una regla de esta feature empieza a mirar {@code code} o
 * {@code name}, se ha roto el limite del slice.
 */
public record CatalogItemRef(Long id, String code, String name) {

    public CatalogItemRef {
        if (id == null)
            throw new IllegalArgumentException("catalog item id is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("catalog item code is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("catalog item name is required");
    }
}
