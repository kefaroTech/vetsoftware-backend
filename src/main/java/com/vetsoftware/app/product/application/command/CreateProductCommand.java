package com.vetsoftware.app.product.application.command;

import java.math.BigDecimal;

public record CreateProductCommand(
        String name,
        String code,
        BigDecimal purchasePrice,
        BigDecimal salePrice,
        Integer currentStock,
        Integer minStock,
        String provider,
        boolean hasTax,
        boolean expireDate,
        String notes,
        Long productCategoryId,
        Long taxId,
        Long companyId
) {}
