package com.vetsoftware.app.vatfilingperiod.infrastructure.web.request;

import com.vetsoftware.app.vatfilingperiod.domain.VatFilingFrequency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateVatFilingPeriodRequest(@Min(2020) @Max(2100) int fiscalYear,
        @NotNull VatFilingFrequency frequency, @NotBlank @Size(max = 255) String legalReference) {
}
