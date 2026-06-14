package com.vetsoftware.app.numberingresolution.application.command;

import com.vetsoftware.app.numberingresolution.domain.ElectronicDocumentType;
import java.time.LocalDate;

public record UpdateNumberingResolutionCommand(
        Long id,
        ElectronicDocumentType documentType,
        String resolutionNumber,
        LocalDate resolutionDate,
        String prefix,
        Long rangeFrom,
        Long rangeTo,
        LocalDate validFrom,
        LocalDate validTo,
        String technicalKey,
        Long companyId) {}
