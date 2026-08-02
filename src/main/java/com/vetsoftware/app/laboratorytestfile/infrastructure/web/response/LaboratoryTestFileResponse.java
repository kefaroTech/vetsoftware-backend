package com.vetsoftware.app.laboratorytestfile.infrastructure.web.response;

import java.time.LocalDateTime;

public record LaboratoryTestFileResponse(
    Long id,
    String storageKey,
    String bucket,
    String originalFileName,
    String contentType,
    Long sizeBytes,
    String eTag,
    EmployeeSummary uploadedBy,
    LaboratoryTestSummary laboratoryTest,
    LocalDateTime createdDate) {}
