package com.vetsoftware.app.pricelist.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Una cabecera funcional del configurador: el codigo con el que los modulos se
 * agrupan y el rotulo que se pinta encima de ellos.
 *
 * <p>
 * <strong>El orden de esta lista ES el orden de presentacion.</strong> No se
 * publica ningun {@code sortOrder} y quien pinta esto <strong>no debe
 * reordenar</strong>: el criterio lo fija el {@code ORDER BY} del servidor,
 * misma convencion que ya rige {@code modules}, {@code capacities} y
 * {@code packs} en esta misma respuesta.
 *
 * <p>
 * Un modulo dice a que area pertenece con
 * {@link PublicCatalogItemResponse#areaCode()}; dentro de cada area, el orden
 * es tambien el de la lista {@code modules}.
 */
public record PublicCatalogAreaResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Codigo del area; es el valor que traen los modulos en areaCode") String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {
}
