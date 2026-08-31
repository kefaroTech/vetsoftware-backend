package com.vetsoftware.app.catalogitemaihint.infrastructure.web.request;

import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHint;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * El texto corregido, y nada mas.
 *
 * <p>
 * <strong>Sin {@code catalogItemId}</strong>: viaja en la ruta, que es la que
 * identifica el recurso «la pista vigente de este articulo». Traerlo tambien en
 * el cuerpo abriria la puerta a que los dos no coincidan y a que alguien
 * tuviera que decidir cual manda.
 *
 * <p>
 * &#9940; <strong>Sin firmante</strong>: la revision nueva la firma el
 * principal de la sesion, no el cuerpo.
 */
public record ReviseCatalogItemAiHintRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "El texto corregido, en las mismas tres partes: que es el modulo,"
                + " las senales literales, y cuando NO aplica") @NotBlank @Size(max = CatalogItemAiHint.MAX_HINT_TEXT) String hintText) {
}
