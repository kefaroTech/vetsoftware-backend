package com.vetsoftware.app.quote.infrastructure.web.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Lo unico que el cliente elige de una linea.
 *
 * <p>
 * Ni precio, ni nombre, ni tarifa de IVA: los resuelve el servidor contra el
 * catalogo y la tarifa. Si el importe viajara aqui, cotizar a cero seria un
 * campo de formulario.
 *
 * @param quantity
 *            unidades CONTRATADAS. Las que se cobran salen de restarle las que
 *            la tarifa ya incluye, y esa resta la hace el dominio.
 */
public record QuoteLineRequest(@NotNull Long catalogItemId, @Positive int quantity,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal discountPercent) {
}
