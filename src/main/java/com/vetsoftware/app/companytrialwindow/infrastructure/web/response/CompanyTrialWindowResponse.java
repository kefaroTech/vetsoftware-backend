package com.vetsoftware.app.companytrialwindow.infrastructure.web.response;

import com.vetsoftware.app.companytrialwindow.application.dto.CompanyTrialWindowDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La ventana de prueba tal como la ven los frontends.
 *
 * <p>
 * {@code endDate} es el <strong>último día en prueba, incluido</strong>, y
 * viaja calculado desde el servidor. Que el front lo derive de
 * {@code startDate + windowDays} es exactamente el desfase de un día en el que
 * ya cayó el documento de diseño: son dos convenciones distintas del mismo
 * número y basta con que una pantalla elija la otra para que el cliente lea una
 * fecha que la base no reconoce.
 *
 * <p>
 * {@code open} viaja calculado por la misma razón que {@code alive} en las
 * excepciones de techo: «viva» es la ausencia de {@code closedAt}, y dejar esa
 * negación a cada pantalla es garantizar que dos pantallas discrepen.
 */
public record CompanyTrialWindowResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate startDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Último día en prueba, incluido") LocalDate endDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int windowDays,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long sourceQuoteId,
        LocalDateTime closedAt, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean open) {

    public static CompanyTrialWindowResponse from(CompanyTrialWindowDto dto) {
        return new CompanyTrialWindowResponse(dto.id(), dto.companyId(), dto.startDate(),
                dto.endDate(), dto.windowDays(), dto.sourceQuoteId(), dto.closedAt(), dto.open());
    }
}
