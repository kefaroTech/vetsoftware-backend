package com.vetsoftware.app.smmlvvalue.infrastructure.web.response;

import com.vetsoftware.app.smmlvvalue.application.dto.SmmlvValueDto;
import com.vetsoftware.app.smmlvvalue.domain.SmmlvStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * El salario minimo de un ano tal como sale por HTTP, <strong>con su
 * estado</strong>.
 *
 * <p>
 * {@code inForce} va calculado para que el front no tenga que conocer el enum
 * para saber si debe pintar la advertencia de «cifra en disputa».
 */
public record SmmlvValueResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "2026") int fiscalYear,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "1750905.00") BigDecimal valueAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Decreto 1469 de 2025") String legalReference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SmmlvStatus status,
        @Schema(description = "Providencia o norma que movio el estado") String statusReference,
        LocalDate statusChangedOn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean inForce,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {

    public static SmmlvValueResponse from(SmmlvValueDto dto) {
        return new SmmlvValueResponse(dto.id(), dto.fiscalYear(), dto.valueAmount(),
                dto.legalReference(), dto.status(), dto.statusReference(), dto.statusChangedOn(),
                dto.inForce(), dto.createdDate(), dto.enabled());
    }
}
