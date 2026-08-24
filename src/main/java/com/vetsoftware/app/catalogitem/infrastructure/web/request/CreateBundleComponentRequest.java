package com.vetsoftware.app.catalogitem.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateBundleComponentRequest(
        @NotNull(message = "Debes seleccionar el artículo que compone el paquete.") Long componentItemId,
        @NotNull(message = "Debes indicar la cantidad.") @Positive(message = "La cantidad debe ser mayor que cero.") Integer quantity) {
}
