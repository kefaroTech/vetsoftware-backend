package com.vetsoftware.app.smmlvvalue.infrastructure.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateSmmlvValueRequest(@Min(2020) @Max(2100) int fiscalYear,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal valueAmount,
        @NotBlank @Size(max = 255) String legalReference) {
}
