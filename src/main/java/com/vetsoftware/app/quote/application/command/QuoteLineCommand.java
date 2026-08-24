package com.vetsoftware.app.quote.application.command;

import java.math.BigDecimal;

/**
 * Lo UNICO que el cliente elige de una linea: que articulo, cuantos y que
 * descuento se negocio.
 *
 * <p>
 * Ni el nombre, ni el precio, ni la tarifa de IVA viajan aqui. Los resuelve el
 * servidor contra el catalogo y la tarifa cotizada en el momento de congelar la
 * linea: si el importe lo pusiera el cliente, cotizar a cero seria un campo de
 * formulario.
 */
public record QuoteLineCommand(Long catalogItemId, int quantity, BigDecimal discountPercent) {
}
