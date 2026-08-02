package com.vetsoftware.app.inventory.application.command;

/** Fija el stock mínimo (punto de reorden) de un producto en UNA sede. */
public record SetMinStockCommand(Long companyId, Long branchId, Long productId, int minStock) {
}
