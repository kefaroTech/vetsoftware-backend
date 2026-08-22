package com.vetsoftware.app.inventory.infrastructure.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Fija el mínimo (punto de reorden) del producto en una sede. */
public record SetMinStockRequest(Long branchId,
        @NotNull(message = "El stock mínimo es obligatorio.") @Min(value = 0, message = "El stock mínimo no puede ser negativo.") Integer minStock) {
}
