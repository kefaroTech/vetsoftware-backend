package com.vetsoftware.app.quote.domain;

import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

/**
 * Los cuatro totales de una oferta, sumados de sus lineas.
 *
 * <p>
 * <strong>Existe para que solo haya una forma de sumar.</strong> Estas cuatro
 * cuentas vivian privadas dentro de {@link Quote}, que las usa para demostrar
 * que su cabecera guardada sigue cuadrando con las lineas (regla R5). Al
 * aparecer un segundo consumidor —la vista previa de precio, que calcula un
 * total sin llegar a crear una oferta— habia que elegir entre copiarlas o
 * sacarlas. Copiarlas es exactamente el defecto que este proyecto acaba de
 * pagar dos veces: el front multiplicando el tramo de entrada y extrapolando el
 * anual, dos cifras plausibles que no eran la que se cobra.
 *
 * <p>
 * <strong>El total no se deriva del subtotal.</strong> Se suma linea a linea,
 * igual que los otros tres, y {@link Quote} comprueba <em>ademas</em> que
 * {@code subtotal - descuento + impuesto} coincide. Son dos caminos hacia el
 * mismo numero y esa redundancia es la comprobacion: si un redondeo se
 * descolgara, los dos dejarian de coincidir.
 *
 * <p>
 * {@code Money.zero()} como semilla del acumulador y no
 * {@code BigDecimal.ZERO}: la escala importa al comparar con {@code compareTo}
 * sobre una lista vacia.
 */
public record QuoteTotals(BigDecimal subtotalAmount, BigDecimal discountAmount,
        BigDecimal taxAmount, BigDecimal totalAmount) {

    /** Los cuatro totales de esas lineas. Lista vacia da cuatro ceros. */
    public static QuoteTotals of(List<QuoteLine> lines) {
        List<QuoteLine> seguras = lines == null ? List.of() : lines;
        return new QuoteTotals(sum(seguras, QuoteLine::grossAmount),
                sum(seguras, QuoteLine::getDiscountAmount), sum(seguras, QuoteLine::getTaxAmount),
                sum(seguras, QuoteLine::getLineTotal));
    }

    private static BigDecimal sum(List<QuoteLine> lines,
            Function<QuoteLine, BigDecimal> extractor) {
        return lines.stream().map(extractor).reduce(Money.zero(), BigDecimal::add);
    }
}
