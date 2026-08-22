package com.vetsoftware.app.inventory.infrastructure.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entrada de mercancía (recepción/compra): crea/acumula un lote con costo y
 * vencimiento en la sede.
 */
public record ReceiveStockRequest(Long branchId,
        @NotNull(message = "Debes seleccionar el producto.") Long productId,
        @NotNull(message = "La cantidad es obligatoria.") @Min(value = 1, message = "La cantidad debe ser de al menos 1 unidad.") Integer quantity,
        @NotNull(message = "El costo unitario es obligatorio.") @DecimalMin(value = "0.0", message = "El costo unitario no puede ser negativo.") BigDecimal unitCost,
        @Size(max = 50, message = "El número de lote no puede superar los 50 caracteres.") String lotNumber,
        LocalDate expireDate) {
}
