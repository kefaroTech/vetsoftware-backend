package com.vetsoftware.app.inventory.infrastructure.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Ajuste por conteo físico. {@code delta} con signo (+entra/−sale, ≠0).
 * {@code unitCost} solo aplica si delta>0.
 */
public record AdjustStockRequest(
        /**
         * Sede del ajuste. Opcional: null = sede única del empleado / Principal (según
         * alcance).
         */
        Long branchId, @NotNull(message = "Debes seleccionar el producto.") Long productId,
        @NotNull(message = "La cantidad del ajuste es obligatoria.") Integer delta,
        @DecimalMin(value = "0.0", message = "El costo unitario no puede ser negativo.") BigDecimal unitCost,
        @NotBlank(message = "El motivo del ajuste es obligatorio.") @Size(max = 255, message = "El motivo del ajuste no puede superar los 255 caracteres.") String reason) {
}
