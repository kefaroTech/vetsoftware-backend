package com.vetsoftware.app.numberingresolution.infrastructure.web.request;

import com.vetsoftware.app.numberingresolution.domain.ElectronicDocumentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateNumberingResolutionRequest(
        @NotNull(message = "Debes seleccionar el tipo de documento electrónico.") ElectronicDocumentType documentType,
        @NotBlank(message = "El número de la resolución es obligatorio.") @Size(max = 50, message = "El número de la resolución no puede superar los 50 caracteres.") String resolutionNumber,
        @NotNull(message = "La fecha de la resolución es obligatoria.") LocalDate resolutionDate,
        @Size(max = 10, message = "El prefijo no puede superar los 10 caracteres.") String prefix,
        @NotNull(message = "El número inicial del rango es obligatorio.") @Min(value = 1, message = "El número inicial del rango debe ser mayor que cero.") Long rangeFrom,
        @NotNull(message = "El número final del rango es obligatorio.") @Min(value = 1, message = "El número final del rango debe ser mayor que cero.") Long rangeTo,
        @NotNull(message = "La fecha de inicio de vigencia es obligatoria.") LocalDate validFrom,
        @NotNull(message = "La fecha de fin de vigencia es obligatoria.") LocalDate validTo,
        @Size(max = 255, message = "La clave técnica no puede superar los 255 caracteres.") String technicalKey,
        // Sede (opcional): prefijo por sucursal. Omitir = resolución de empresa (todas
        // las sedes).
        Long branchId) {
}
