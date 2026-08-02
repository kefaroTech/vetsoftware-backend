package com.vetsoftware.app.inventory.infrastructure.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Consumo clínico manual de un producto desde una sede. */
public record ConsumeStockRequest(Long branchId, @NotNull Long productId,
        @NotNull @Min(1) Integer quantity, @Size(max = 255) String reason) {
}
