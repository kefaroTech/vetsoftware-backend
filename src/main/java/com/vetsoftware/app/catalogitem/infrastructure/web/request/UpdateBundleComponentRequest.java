package com.vetsoftware.app.catalogitem.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateBundleComponentRequest(
        @NotNull(message = "Debes indicar la cantidad.") @Positive(message = "La cantidad debe ser mayor que cero.") Integer quantity) {
}
