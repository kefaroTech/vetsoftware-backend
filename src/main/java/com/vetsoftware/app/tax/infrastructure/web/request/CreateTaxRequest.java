package com.vetsoftware.app.tax.infrastructure.web.request;

import com.vetsoftware.app.tax.domain.TaxScheme;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateTaxRequest(@NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal percentage,
        @NotNull TaxScheme taxScheme) {
}
