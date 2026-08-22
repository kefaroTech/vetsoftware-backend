package com.vetsoftware.app.inventory.infrastructure.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Transferencia entre sedes. {@code fromBranchId} debe estar en el alcance del
 * empleado; destino, misma empresa.
 */
public record TransferStockRequest(
        @NotNull(message = "Debes seleccionar la sede de origen.") Long fromBranchId,
        @NotNull(message = "Debes seleccionar la sede de destino.") Long toBranchId,
        @NotNull(message = "Debes seleccionar el producto.") Long productId,
        @NotNull(message = "La cantidad es obligatoria.") @Min(value = 1, message = "La cantidad debe ser de al menos 1 unidad.") Integer quantity,
        @Size(max = 255, message = "El motivo no puede superar los 255 caracteres.") String reason) {
}
