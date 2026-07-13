package com.vetsoftware.app.inventory.infrastructure.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Fija el mínimo (punto de reorden) del producto en una sede. */
public record SetMinStockRequest(
        Long branchId,
        @NotNull @Min(0) Integer minStock
) {}
