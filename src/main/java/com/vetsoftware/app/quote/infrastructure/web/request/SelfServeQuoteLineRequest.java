package com.vetsoftware.app.quote.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

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
 *
 * <p>
 * <strong>El articulo se nombra por {@code code}, no por id, y esa es la unica
 * forma de que este endpoint sea alcanzable.</strong> Lo que un empleado del
 * tenant puede ver del catalogo es {@code GET /plans}, que
 * <em>deliberadamente</em> no publica ningun id —«un id es una llave de
 * escritura y un {@code code} es un rotulo», dice {@code PublicPlanResponse}—,
 * y el unico traductor {@code code -> id} que existia,
 * {@code GET /catalog-items}, esta cerrado a {@code hasRole('SYSTEM')}. Con
 * {@code catalogItemId} en el cuerpo, la autocontratacion tenia permiso
 * sembrado, ruta publicada y <b>cero llamadores posibles</b>: no habia ninguna
 * cadena por la que un tenant llegara a esos numeros.
 *
 * <p>
 * <strong>Y el id no era solo inutil aqui: era un oraculo.</strong> Un empleado
 * con {@code quote.request} podia recorrer 1, 2, 3… y leer en la respuesta el
 * {@code itemCode} y el {@code itemName} de cada articulo {@code ACTIVE} del
 * catalogo interno —incluidos los {@code ONE_TIME} de implantacion, que
 * {@code GET /plans} no publica a proposito—, distinguiendo lo que existe de lo
 * que no por la forma del error. El {@code code} cierra esa puerta porque
 * {@code SelfServeQuoteService} lo resuelve <b>solo</b> contra el conjunto que
 * la portada ya publica, y responde lo mismo para un codigo inexistente que
 * para uno interno.
 *
 * <p>
 * El {@code @Size(max = 50)} no es decorativo: es exactamente el ancho de
 * {@code catalog_items.code}. Un codigo mas largo no puede casar con ninguna
 * fila, asi que se rechaza en el borde como error de campo en vez de gastar una
 * consulta.
 *
 * <p>
 * <strong>Y el {@code min = 1} tampoco lo es.</strong> El {@code @NotBlank} ya
 * rechaza la cadena vacia en el servidor, pero springdoc <b>no lo lee</b>:
 * deriva el {@code minLength} del {@code @Size} y solo de el. Con
 * {@code @Size(max = 50)} a secas, el contrato publicado anunciaba
 * {@code minLength: 0} —es decir, «la cadena vacia es un codigo valido»—
 * mientras el servidor la rechazaba con un 400. Un contrato que describe de
 * menos no es conservador: es falso, y los dos fronts generan sus tipos de ahi.
 */
public record SelfServeQuoteLineRequest(@NotBlank @Size(min = 1, max = 50) String code,
        @Positive int quantity) {
}
