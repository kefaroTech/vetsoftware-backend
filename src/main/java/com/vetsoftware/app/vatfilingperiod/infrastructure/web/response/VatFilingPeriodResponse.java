package com.vetsoftware.app.vatfilingperiod.infrastructure.web.response;

import com.vetsoftware.app.vatfilingperiod.application.dto.VatFilingPeriodDto;
import com.vetsoftware.app.vatfilingperiod.domain.VatFilingFrequency;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record VatFilingPeriodResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "2026") int fiscalYear,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) VatFilingFrequency frequency,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String legalReference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {

    public static VatFilingPeriodResponse from(VatFilingPeriodDto dto) {
        return new VatFilingPeriodResponse(dto.id(), dto.fiscalYear(), dto.frequency(),
                dto.legalReference(), dto.createdDate(), dto.enabled());
    }
}
