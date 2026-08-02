package com.vetsoftware.app.inventory.application.command;

/**
 * Lotes disponibles de un producto en una sede (orden FEFO), para
 * UI/trazabilidad.
 */
public record ListLotsCommand(Long companyId, Long branchId, Long productId) {
}
