package com.vetsoftware.app.product.infrastructure.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.vetsoftware.app.product.domain.TaxTreatment;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateProductRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 50) String code,
        @NotNull @DecimalMin("0.0") BigDecimal purchasePrice,
        @NotNull @DecimalMin("0.0") BigDecimal salePrice,
        @NotNull @Min(0) Integer currentStock,
        @NotNull @Min(0) Integer minStock,
        @Size(max = 150) String provider,
        LocalDate expireDate,
        @Size(max = 50) String lotNumber,
        @Size(max = 500) String notes,
        @NotNull TaxTreatment taxTreatment,
        @NotNull Long productCategoryId,
        Long taxId
) {}
