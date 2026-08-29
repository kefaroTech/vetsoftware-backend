package com.vetsoftware.app.quote.application.command;

/**
 * Lo <strong>unico</strong> que una clinica elige de una linea cuando se cotiza
 * a si misma: que articulo y cuantos.
 *
 * <p>
 * Comparelo con {@link QuoteLineCommand}, que ademas lleva
 * {@code discountPercent} y {@code discountIsConditional}. Esos dos campos son
 * negociacion comercial y por eso viajan en el camino de plataforma; aqui
 * <strong>no existen</strong>, y no estan puestos a cero por convenio sino
 * ausentes del tipo. Es la diferencia entre «el servidor los ignora» —que dura
 * hasta el primer refactor distraido— y «no se pueden expresar».
 *
 * <p>
 * Ni el precio, ni el nombre, ni la tarifa de IVA, ni el tramo: los resuelve el
 * servidor contra el catalogo y la tarifa vigente al congelar la linea.
 *
 * <p>
 * <strong>El articulo viaja como {@code code} y no como id.</strong> El id es
 * la llave de escritura del catalogo y el tenant no tiene —ni debe tener—
 * ninguna via para conocerlo: {@code GET /plans} publica rotulos y
 * {@code GET /catalog-items} es de {@code SYSTEM}. La traduccion
 * {@code code -> id} la hace {@code SelfServeQuoteService} contra el conjunto
 * publicado, y ese es el <em>unico</em> punto del camino donde el id llega a
 * existir. {@link QuoteLineCommand} lo conserva porque el camino de plataforma
 * lo emite {@code SYSTEM}, que si conoce el catalogo entero.
 */
public record SelfServeQuoteLineCommand(String code, int quantity) {
}
