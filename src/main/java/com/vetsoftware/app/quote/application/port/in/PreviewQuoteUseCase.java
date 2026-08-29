package com.vetsoftware.app.quote.application.port.in;

import com.vetsoftware.app.quote.application.command.PreviewQuoteCommand;
import com.vetsoftware.app.quote.application.dto.QuotePreviewDto;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Cuanto costaria esta seleccion, sin crear nada.
 *
 * <p>
 * <strong>Por que el servidor calcula en vez de publicar la escalera.</strong>
 * Los tramos son acumulativos y la escalera completa es la politica de
 * descuento por volumen: {@code GET /plans} y {@code GET /catalog} publican
 * solo el tramo de entrada, y {@code PublicPlanQueryPortIT} tiene una prueba
 * que se pone roja si alguien quita ese {@code tier_min = 1}. Con solo el tramo
 * de entrada, un front no puede hacer mas que extrapolar — quince usuarios le
 * salen 156.000 y la cotizacion cobra 141.000— y esta noche ya se pagaron dos
 * defectos de esa misma forma: multiplicar el tramo, y sacar el anual del
 * mensual por diez. Devolver el importe ya calculado cierra las dos cosas:
 * <strong>el front no calcula precios, los pide</strong>, y la politica sigue
 * sin publicarse.
 *
 * <p>
 * <strong>No persiste nada y no es una oferta.</strong> No hay numero, ni
 * vigencia, ni estado, ni nada que aceptar; para eso esta
 * {@link SelfServeQuoteUseCase}, que ademas exige estar autenticado. Aqui solo
 * se responde una pregunta.
 *
 * <p>
 * <strong>Hacer publica una ruta en este proyecto son DOS cosas, y esta ademas
 * es un POST, asi que son TRES.</strong> La anotacion de aqui; la entrada
 * {@code new Route(HttpMethod.POST, "/quotes/preview")} en
 * {@code PublicRoutes.BUSINESS}; y su propio limite por IP en
 * {@code LoginRateLimitFilter}, porque {@code LoginRateLimitFilterTest} recorre
 * {@code PublicRoutes.BUSINESS} y rompe el build ante cualquier {@code POST}
 * publico sin limitar. Sin la ruta el prospecto se lleva un 401; sin el limite,
 * el build no pasa; sin la anotacion, tampoco.
 *
 * <p>
 * Aplica el mismo gate que la autocontratacion —solo rotulos publicados, sin
 * cobro doble y sin cesta incoherente—, asi que no ensena nada que
 * {@code GET /catalog} no ensene ya.
 */
@NoAuthorizationRequired(reason = "Es la calculadora de la landing comercial: un prospecto sin cuenta tiene que poder ver cuanto le costaria su seleccion antes de dar su NIT. No persiste nada, no crea ninguna oferta y no devuelve dato alguno de ninguna empresa -las tablas del catalogo comercial no tienen company_id-. Resuelve exclusivamente los rotulos que GET /catalog ya publica, con el mismo gate que la autocontratacion, y lleva su propio limite por IP en LoginRateLimitFilter.")
public interface PreviewQuoteUseCase {

    /**
     * El desglose y los cuatro totales de esa seleccion, en la tarifa vigente hoy y
     * el ciclo pedido.
     */
    QuotePreviewDto preview(PreviewQuoteCommand command);
}
