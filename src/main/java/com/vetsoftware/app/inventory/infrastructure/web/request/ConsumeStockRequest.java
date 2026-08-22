package com.vetsoftware.app.inventory.infrastructure.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Consumo clínico manual de un producto desde una sede. */
public record ConsumeStockRequest(Long branchId,
        @NotNull(message = "Debes seleccionar el producto.") Long productId,
        @NotNull(message = "La cantidad es obligatoria.") @Min(value = 1, message = "La cantidad debe ser de al menos 1 unidad.") Integer quantity,
        @Size(max = 255, message = "El motivo no puede superar los 255 caracteres.") String reason) {
}
