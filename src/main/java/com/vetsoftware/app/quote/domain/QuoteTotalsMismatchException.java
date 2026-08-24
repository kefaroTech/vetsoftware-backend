package com.vetsoftware.app.quote.domain;

import java.math.BigDecimal;

/**
 * Los totales guardados no cuadran con la suma de las lineas (regla R5).
 *
 * <p>
 * Que se rompe si no se comprueba: el cliente firma un numero y se le factura
 * otro. La cabecera guarda los cuatro totales para que un cambio futuro de
 * redondeo no mueva un documento viejo; el precio de guardarlos es que hay que
 * demostrar que cuadran, y eso es lo que hace esta excepcion.
 *
 * <p>
 * Un cliente no deberia verla nunca: los totales los calcula el servidor desde
 * las lineas y nadie los envia. Si salta, es corrupcion de datos o un defecto
 * propio, no una entrada mala.
 */
public class QuoteTotalsMismatchException extends IllegalStateException {
    public QuoteTotalsMismatchException(String concept, BigDecimal stored, BigDecimal fromLines) {
        super("Quote totals mismatch on " + concept + ": stored=" + stored + " lines=" + fromLines);
    }
}
