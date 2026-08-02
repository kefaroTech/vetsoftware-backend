package com.vetsoftware.app.inventory.infrastructure.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Transferencia entre sedes. {@code fromBranchId} debe estar en el alcance del
 * empleado; destino, misma empresa.
 */
public record TransferStockRequest(@NotNull Long fromBranchId, @NotNull Long toBranchId,
        @NotNull Long productId, @NotNull @Min(1) Integer quantity,
        @Size(max = 255) String reason) {
}
