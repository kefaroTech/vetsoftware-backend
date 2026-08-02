package com.vetsoftware.app.service.infrastructure.web.request;

import com.vetsoftware.app.service.domain.TaxTreatment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateServiceRequest(@NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin("0.0") BigDecimal price, @Size(max = 500) String notes,
        @NotNull TaxTreatment taxTreatment, @NotNull Long serviceCategoryId, Long taxId) {
}
