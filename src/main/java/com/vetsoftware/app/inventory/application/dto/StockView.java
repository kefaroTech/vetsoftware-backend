package com.vetsoftware.app.inventory.application.dto;

/** Saldo por (producto, sede) para lectura: incluye nombres de producto/sede y el flag de bajo mínimo. */
public record StockView(
        Long productId,
        String productName,
        String productCode,
        Long branchId,
        String branchName,
        int quantity,
        int minStock,
        boolean lowStock
) {}
