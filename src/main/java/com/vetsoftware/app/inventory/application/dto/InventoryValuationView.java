package com.vetsoftware.app.inventory.application.dto;

import java.math.BigDecimal;
import java.util.List;

/** Valuación total del inventario (una sede o todas) + desglose por producto. */
public record InventoryValuationView(
        BigDecimal totalValue,
        int totalUnits,
        List<ProductValuationView> byProduct
) {}
