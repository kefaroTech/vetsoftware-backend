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
 *            la tarifa ya incluye, y esa resta la hace el dominio. Si la tarifa
 *            del articulo tiene escalones, esa cantidad se reparte
 *            acumulativamente entre ellos y produce VARIOS renglones (D-66).
 * @param discountIsConditional
 *            D-86. Marca el descuento como sujeto a condicion -permanencia-, y
 *            entonces el IVA se liquida sobre el precio de lista y no sobre el
 *            rebajado. Ausente es {@code false}: lo caro es marcar de mas, no
 *            de menos.
 *            <p>
 *            Es {@code Boolean} y no {@code boolean} a proposito: un primitivo
 *            ausente del cuerpo no se deja en su valor por defecto, se rechaza
 *            con un error de campo -«el valor enviado no es valido»- sobre un
 *            campo que el cliente ni menciono. Con el envoltorio, omitirlo es
 *            legal y significa lo que tiene que significar.
 */
public record QuoteLineRequest(@NotNull Long catalogItemId, @Positive int quantity,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal discountPercent,
        Boolean discountIsConditional) {
}
