package com.vetsoftware.app.product.application.dto;

import com.vetsoftware.app.product.domain.Product;
import com.vetsoftware.app.product.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductDto(Long id, String name, String code, BigDecimal salePrice,
        String baseUnitMeasureCode, String provider, SupplierSummaryDto supplier,
        TaxTreatment taxTreatment, String notes, ProductCategorySummaryDto productCategory,
        TaxSummaryDto tax, CompanySummaryDto company, LocalDateTime createdDate,
        LocalDateTime updatedDate, Long updatedBy, Long version, boolean enabled) {
    public static ProductDto from(Product product) {
        return new ProductDto(product.getId(), product.getName(), product.getCode(),
                product.getSalePrice(), product.getBaseUnitMeasureCode(), product.getProvider(),
                product.getSupplier() == null
                        ? null
                        : SupplierSummaryDto.from(product.getSupplier()),
                product.getTaxTreatment(), product.getNotes(),
                ProductCategorySummaryDto.from(product.getProductCategory()),
                product.getTax() == null ? null : TaxSummaryDto.from(product.getTax()),
                CompanySummaryDto.from(product.getCompany()), product.getCreatedDate(),
                product.getUpdatedDate(), product.getUpdatedBy(), product.getVersion(),
                product.isEnabled());
    }
}
