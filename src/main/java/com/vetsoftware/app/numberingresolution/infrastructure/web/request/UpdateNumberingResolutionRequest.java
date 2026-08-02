package com.vetsoftware.app.numberingresolution.infrastructure.web.request;

import com.vetsoftware.app.numberingresolution.domain.ElectronicDocumentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateNumberingResolutionRequest(
    @NotNull ElectronicDocumentType documentType,
    @NotBlank @Size(max = 50) String resolutionNumber,
    @NotNull LocalDate resolutionDate,
    @Size(max = 10) String prefix,
    @NotNull @Min(1) Long rangeFrom,
    @NotNull @Min(1) Long rangeTo,
    @NotNull LocalDate validFrom,
    @NotNull LocalDate validTo,
    @Size(max = 255) String technicalKey,
    // Sede (opcional): prefijo por sucursal. Omitir = resolución de empresa (todas las sedes).
    Long branchId) {}
