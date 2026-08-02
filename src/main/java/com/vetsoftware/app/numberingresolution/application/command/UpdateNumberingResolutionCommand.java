package com.vetsoftware.app.numberingresolution.application.command;

import com.vetsoftware.app.numberingresolution.domain.ElectronicDocumentType;
import java.time.LocalDate;

public record UpdateNumberingResolutionCommand(Long id, ElectronicDocumentType documentType,
        String resolutionNumber, LocalDate resolutionDate, String prefix, Long rangeFrom,
        Long rangeTo, LocalDate validFrom, LocalDate validTo, String technicalKey,
        // Multi-sucursal (B-6): sede a la que aplica el prefijo. null = resolución de
        // empresa (todas
        // las sedes).
        Long branchId, Long companyId) {
}
