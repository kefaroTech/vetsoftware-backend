package com.vetsoftware.app.inventory.infrastructure.persistence;

/** Proyección de la búsqueda de saldo (stock_balance ⋈ products ⋈ branches). Se mapea por alias de columna. */
public interface StockRow {
    Long getProductId();
    String getProductName();
    String getProductCode();
    Long getBranchId();
    String getBranchName();
    int getQuantity();
    int getMinStock();
}
