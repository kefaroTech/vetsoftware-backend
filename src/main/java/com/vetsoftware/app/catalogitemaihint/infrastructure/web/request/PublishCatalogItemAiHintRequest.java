package com.vetsoftware.app.catalogitemaihint.infrastructure.web.request;

import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHint;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <strong>Sin {@code hintRevision}</strong>: el numero lo asigna el servidor a
 * partir del ultimo publicado. Un cliente que pudiera elegirlo podria saltarse
 * el orden del historial o chocar contra
 * {@code uq_catalog_item_ai_hints_revision} sin entender por que.
 *
 * <p>
 * &#9940; <strong>Y sin {@code publishedBySystemUserId}</strong>: lo pone el
 * controller desde el principal ({@code authz.currentSystemUserId()}). Un
 * rastro de auditoria que escribe el auditado no es un rastro de auditoria.
 *
 * <p>
 * Tampoco lleva {@code companyId}, y no podria: la tabla no lo tiene.
 */
public record PublishCatalogItemAiHintRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Articulo del catalogo al que se le publica la pista") @NotNull Long catalogItemId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "El texto de la pista, en tres partes separadas por linea en blanco:"
                + " que es el modulo en palabras del negocio, las senales literales en el"
                + " texto del prospecto, y cuando NO aplica (el contraejemplo, sin el cual"
                + " el modelo propone de mas)") @NotBlank @Size(max = CatalogItemAiHint.MAX_HINT_TEXT) String hintText) {
}
