package com.vetsoftware.app.configurator.application.dto;

/**
 * Una linea del carrito que produce el cuestionario: <strong>rotulo</strong> y
 * cantidad.
 *
 * <p>
 * <strong>El {@code code} sustituye al {@code catalogItemId}, y no es un
 * renombrado.</strong> Son dos decisiones a la vez:
 *
 * <ul>
 * <li><b>Se cierra el desajuste.</b> {@code GET /plans}, {@code GET /catalog} y
 * {@code POST /quotes/self-serve} hablan todos de {@code code}. Mientras esto
 * devolviera ids, lo que resolvia el configurador no se podia tarifar contra
 * ningun catalogo publicado ni enviar a contratar: eran dos mitades que no
 * encajaban.</li>
 * <li><b>Se quita un oraculo de enumeracion.</b> Un id secuencial en una
 * respuesta anonima deja sondear el catalogo entero. Es el mismo motivo por el
 * que la autocontratacion dejo de aceptarlo y por el que
 * {@code PublicPlanResponse} no publica ni uno.</li>
 * </ul>
 */
public record SelectedItemDto(String code, int quantity) {
}
