package com.vetsoftware.app.product.infrastructure.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 50) String code,
        @NotNull @DecimalMin("0.0") BigDecimal purchasePrice,
        @NotNull @DecimalMin("0.0") BigDecimal salePrice,
        @NotNull @Min(0) Integer currentStock,
        @NotNull @Min(0) Integer minStock,
        @Size(max = 150) String provider,
        boolean hasTax,
        boolean expireDate,
        @Size(max = 500) String notes,
        @NotNull Long productCategoryId,
        Long taxId
) {}
