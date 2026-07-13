package com.vetsoftware.app.product.infrastructure.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.vetsoftware.app.product.domain.TaxTreatment;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 50) String code,
        @NotNull @DecimalMin("0.0") BigDecimal salePrice,
        @Size(max = 150) String provider,
        @Size(max = 500) String notes,
        @NotNull TaxTreatment taxTreatment,
        @NotNull Long productCategoryId,
        Long taxId
) {}
