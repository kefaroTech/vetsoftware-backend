package com.vetsoftware.app.product.infrastructure.web.response;

import com.vetsoftware.app.product.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
    Long id,
    String name,
    String code,
    BigDecimal salePrice,
    String baseUnitMeasureCode,
    String provider,
    SupplierSummary supplier,
    TaxTreatment taxTreatment,
    String notes,
    ProductCategorySummary productCategory,
    TaxSummary tax,
    CompanySummary company,
    LocalDateTime createdDate,
    LocalDateTime updatedDate,
    Long updatedBy,
    Long version,
    boolean enabled) {}
