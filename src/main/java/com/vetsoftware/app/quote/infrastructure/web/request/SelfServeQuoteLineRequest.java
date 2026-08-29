package com.vetsoftware.app.quote.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Que articulo y cuantos. Nada mas, y esa es la diferencia entera con
 * {@link QuoteLineRequest}.
 *
 * <p>
 * Aquel lleva {@code discountPercent} y {@code discountIsConditional} porque el
 * camino de plataforma cotiza con descuentos negociados. Aqui esos dos campos
 * <strong>no existen en el tipo</strong>: no se validan a cero, no se ignoran,
 * no se sobreescriben. Un cuerpo que los mande recibe el trato que Jackson da a
 * un campo desconocido, y ningun refactor futuro puede hacer que empiecen a
 * contar, porque no hay donde escribirlos.
 */
public record SelfServeQuoteLineRequest(@NotNull Long catalogItemId, @Positive int quantity) {
}
