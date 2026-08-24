package com.vetsoftware.app.configurator.domain;

/**
 * Un artículo del carrito con su cantidad, tal como lo deja el configurador.
 *
 * <p>
 * Lleva el id del artículo y nada más del catálogo: el precio, el nombre y el
 * tipo los congela {@code quote_lines} en el momento de cotizar, con la lista
 * de precios de esa oferta. Copiarlos aquí sería una segunda verdad sobre lo
 * mismo.
 *
 * @param catalogItemId
 *            el artículo
 * @param quantity
 *            cuántas unidades; siempre mayor que cero — una cantidad cero no es
 *            una línea, es la ausencia de línea, y se descarta antes de llegar
 *            aquí
 */
public record SelectedItem(Long catalogItemId, int quantity) {

    public SelectedItem {
        if (catalogItemId == null) {
            throw new IllegalArgumentException("catalogItemId is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
    }
}
