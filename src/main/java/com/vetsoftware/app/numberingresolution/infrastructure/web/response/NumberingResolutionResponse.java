package com.vetsoftware.app.numberingresolution.infrastructure.web.response;

import com.vetsoftware.app.numberingresolution.domain.ElectronicDocumentType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record NumberingResolutionResponse(
        Long id,
        CompanySummary company,
        ElectronicDocumentType documentType,
        String resolutionNumber,
        LocalDate resolutionDate,
        String prefix,
        Long rangeFrom,
        Long rangeTo,
        LocalDate validFrom,
        LocalDate validTo,
        String technicalKey,
        Long currentNumber,
        LocalDateTime createdDate,
        boolean enabled
) {}
