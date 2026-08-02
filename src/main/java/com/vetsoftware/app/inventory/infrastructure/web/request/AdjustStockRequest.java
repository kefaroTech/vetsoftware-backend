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
        Long branchId, @NotNull Long productId, @NotNull Integer delta,
        @DecimalMin("0.0") BigDecimal unitCost, @NotBlank @Size(max = 255) String reason) {
}
