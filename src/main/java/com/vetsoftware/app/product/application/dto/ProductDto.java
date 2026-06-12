package com.vetsoftware.app.product.application.dto;

import com.vetsoftware.app.product.domain.Product;
import com.vetsoftware.app.product.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductDto(
        Long id,
        String name,
        String code,
        BigDecimal purchasePrice,
        BigDecimal salePrice,
        Integer currentStock,
        Integer minStock,
        String provider,
        TaxTreatment taxTreatment,
        boolean expireDate,
        String notes,
        ProductCategorySummaryDto productCategory,
        TaxSummaryDto tax,
        CompanySummaryDto company,
        LocalDateTime createdDate,
        boolean enabled
) {
    public static ProductDto from(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getCode(),
                product.getPurchasePrice(),
                product.getSalePrice(),
                product.getCurrentStock(),
                product.getMinStock(),
                product.getProvider(),
                product.getTaxTreatment(),
                product.isExpireDate(),
                product.getNotes(),
                ProductCategorySummaryDto.from(product.getProductCategory()),
                product.getTax() == null ? null : TaxSummaryDto.from(product.getTax()),
                CompanySummaryDto.from(product.getCompany()),
                product.getCreatedDate(),
                product.isEnabled());
    }
}
