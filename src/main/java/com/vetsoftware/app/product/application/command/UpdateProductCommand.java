package com.vetsoftware.app.product.application.command;

import com.vetsoftware.app.product.domain.TaxTreatment;
import java.math.BigDecimal;

public record UpdateProductCommand(
        Long id,
        String name,
        String code,
        BigDecimal salePrice,
        String provider,
        String notes,
        TaxTreatment taxTreatment,
        Long productCategoryId,
        Long taxId,
        Long companyId,
        Long updatedBy,
        Long version
) {}
