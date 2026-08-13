package com.vetsoftware.app.numberingresolution.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.numberingresolution.domain.ElectronicDocumentType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record NumberingResolutionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company, Long branchId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ElectronicDocumentType documentType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String resolutionNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate resolutionDate,
        String prefix, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long rangeFrom,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long rangeTo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate validFrom,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate validTo, String technicalKey,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long currentNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
