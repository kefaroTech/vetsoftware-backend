package com.vetsoftware.app.laboratorytestfile.application.dto;

import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFile;
import java.time.LocalDateTime;

public record LaboratoryTestFileDto(
        Long id,
        String storageKey,
        String bucket,
        String originalFileName,
        String contentType,
        Long sizeBytes,
        String eTag,
        EmployeeSummaryDto uploadedBy,
        LaboratoryTestSummaryDto laboratoryTest,
        LocalDateTime createdDate
) {
    public static LaboratoryTestFileDto from(LaboratoryTestFile file) {
        return new LaboratoryTestFileDto(
            file.getId(),
            file.getStorageKey(),
            file.getBucket(),
            file.getOriginalFileName(),
            file.getContentType(),
            file.getSizeBytes(),
            file.getETag(),
            EmployeeSummaryDto.from(file.getUploadedBy()),
            LaboratoryTestSummaryDto.from(file.getLaboratoryTest()),
            file.getCreatedDate()
        );
    }
}
