package com.vetsoftware.app.configurator.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Una linea del carrito, por rotulo.
 *
 * <p>
 * <strong>Sin ningun id</strong>, como el resto de la superficie publica: un id
 * es una llave de escritura y, secuencial y en un endpoint anonimo, un oraculo
 * con el que enumerar el catalogo. El {@code code} es ademas el mismo rotulo
 * que aceptan {@code POST /quotes/self-serve} y que publica
 * {@code GET /catalog}, asi que esta respuesta ya se puede tarifar y contratar
 * sin traducir nada.
 */
public record SelectedItemResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Rotulo del articulo; el mismo que aceptan /catalog y /quotes/self-serve") String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int quantity) {
}
