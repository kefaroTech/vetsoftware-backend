package com.vetsoftware.app.laboratorytestfile.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record LaboratoryTestFileResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String storageKey,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String bucket,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String originalFileName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String contentType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long sizeBytes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String eTag,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) EmployeeSummary uploadedBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LaboratoryTestSummary laboratoryTest,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {
}
