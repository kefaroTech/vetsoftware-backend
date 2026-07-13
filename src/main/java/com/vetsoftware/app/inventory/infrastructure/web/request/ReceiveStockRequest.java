package com.vetsoftware.app.inventory.infrastructure.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Entrada de mercancía (recepción/compra): crea/acumula un lote con costo y vencimiento en la sede. */
public record ReceiveStockRequest(
        Long branchId,
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity,
        @NotNull @DecimalMin("0.0") BigDecimal unitCost,
        @Size(max = 50) String lotNumber,
        LocalDate expireDate
) {}
