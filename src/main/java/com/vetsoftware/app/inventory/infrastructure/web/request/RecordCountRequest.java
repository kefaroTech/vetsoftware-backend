package com.vetsoftware.app.inventory.infrastructure.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

/**
 * Hoja de conteo físico: sede + líneas contadas por producto. {@code branchId}
 * lo acota el controller al alcance.
 */
public record RecordCountRequest(@NotNull(message = "Debes seleccionar la sede.") Long branchId,
        String note,
        @NotEmpty(message = "Debes asignar al menos un producto al conteo.") @Valid List<Line> lines) {
    public record Line(@NotNull(message = "Debes seleccionar el producto.") Long productId,
            @PositiveOrZero(message = "La cantidad contada no puede ser negativa.") int countedQuantity) {
    }
}
