package com.vetsoftware.app.inventory.application.command;

/**
 * Búsqueda paginada del saldo por (producto, sede). {@code branchId} null =
 * todas las sedes accesibles (el controller ya resolvió el alcance). {@code q}
 * filtra por nombre/código de producto. {@code lowStock}=true → solo bajo
 * mínimo.
 */
public record SearchStockCommand(Long companyId, Long branchId, String q, boolean lowStock,
        int page, int pageSize) {
}
