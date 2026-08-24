package com.vetsoftware.app.configurator.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Lo que el configurador mete en el carrito. Solo ids y cantidades: los precios
 * los pone {@code quote} con la lista vigente, y publicarlos aquí crearía una
 * segunda verdad sobre lo que vale cada cosa.
 */
public record ConfiguratorSelectionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<SelectedItemResponse> items) {
}
