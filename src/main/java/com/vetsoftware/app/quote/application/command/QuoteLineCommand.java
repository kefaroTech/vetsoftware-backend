package com.vetsoftware.app.quote.application.command;

import java.math.BigDecimal;

/**
 * Lo UNICO que el cliente elige de una linea: que articulo, cuantos y que
 * descuento se negocio -y si ese descuento esta condicionado-.
 *
 * <p>
 * Ni el nombre, ni el precio, ni la tarifa de IVA viajan aqui. Los resuelve el
 * servidor contra el catalogo y la tarifa cotizada en el momento de congelar la
 * linea: si el importe lo pusiera el cliente, cotizar a cero seria un campo de
 * formulario.
 *
 * @param discountIsConditional
 *            D-86. Un descuento sujeto a condicion -permanencia- no reduce la
 *            base del IVA. Viaja con el porcentaje porque es la MISMA
 *            negociacion comercial, no un dato del catalogo: quien decide si
 *            hay permanencia es quien decide cuanto se rebaja. Lo que no puede
 *            viajar es el importe.
 */
public record QuoteLineCommand(Long catalogItemId, int quantity, BigDecimal discountPercent,
        boolean discountIsConditional) {

    /** La linea sin descuento condicionado, que es todo el catalogo de hoy. */
    public QuoteLineCommand(Long catalogItemId, int quantity, BigDecimal discountPercent) {
        this(catalogItemId, quantity, discountPercent, false);
    }
}
