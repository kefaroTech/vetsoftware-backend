package com.vetsoftware.app.product.application.command;

import com.vetsoftware.app.product.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateProductCommand(
        Long id,
        String name,
        String code,
        BigDecimal purchasePrice,
        BigDecimal salePrice,
        Integer currentStock,
        Integer minStock,
        String provider,
        LocalDate expireDate,
        String lotNumber,
        String notes,
        TaxTreatment taxTreatment,
        Long productCategoryId,
        Long taxId,
        Long companyId,
        Long updatedBy,
        Long version
) {}
