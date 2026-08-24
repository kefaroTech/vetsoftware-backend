package com.vetsoftware.app.quote.domain;

import java.math.BigDecimal;

/**
 * La aritmetica congelada de una linea no se sostiene: el descuento no es el
 * porcentaje declarado, el IVA no sale de la base, o el total de la linea no es
 * base menos descuento mas IVA.
 *
 * <p>
 * Se comprueba tambien AL LEER de la base, no solo al escribir. Una linea
 * congelada solo vale como prueba si sus cinco importes siguen contando la
 * misma historia; si alguien los edito por SQL, esto lo delata en el acto en
 * vez de dejar que el descuadre aparezca en la factura.
 */
public class QuoteLineArithmeticException extends IllegalStateException {
    public QuoteLineArithmeticException(String concept, BigDecimal stored, BigDecimal recomputed) {
        super("Quote line arithmetic mismatch on " + concept + ": stored=" + stored + " recomputed="
                + recomputed);
    }
}
