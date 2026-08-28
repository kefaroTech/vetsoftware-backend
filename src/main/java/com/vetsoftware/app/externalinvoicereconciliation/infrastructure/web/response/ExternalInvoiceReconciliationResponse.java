package com.vetsoftware.app.externalinvoicereconciliation.infrastructure.web.response;

import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La conciliacion tal como sale por HTTP. <strong>La ve solo la consola de
 * plataforma</strong>: no hay controller de tenant en este bloque, y por eso
 * este {@code record} publica sin filtro los cuatro numeros enfrentados, la
 * resolucion de numeracion del tercero y la nota interna de quien resolvio.
 *
 * <p>
 * <strong>Si algun dia se abre la lectura al cliente, esta clase NO es la que
 * se reutiliza.</strong> Ensenar {@code computedTax} junto a
 * {@code externalTax} es ensenar el margen y el detalle fiscal de un tercero;
 * la apertura empieza por un {@code record} distinto y recortado, no por
 * relajar el {@code @PreAuthorize} del puerto. El razonamiento completo esta en
 * {@code FindExternalInvoiceReconciliationUseCase}.
 */
public record ExternalInvoiceReconciliationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long billingDocumentId,
        String externalResolutionNumber, Integer externalRangeFrom, Integer externalRangeTo,
        LocalDate resolutionValidUntil, String externalInvoiceId, String externalCufe,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal computedTotal,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal computedTax,
        BigDecimal externalTotal, BigDecimal externalTax, BigDecimal difference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ExternalInvoiceReconciliationStatus status,
        Long resolvedBySystemUserId, LocalDateTime resolvedAt, String resolutionNote,
        String postingPeriod,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static ExternalInvoiceReconciliationResponse from(ExternalInvoiceReconciliationDto dto) {
        return new ExternalInvoiceReconciliationResponse(dto.id(), dto.companyId(),
                dto.billingDocumentId(), dto.externalResolutionNumber(), dto.externalRangeFrom(),
                dto.externalRangeTo(), dto.resolutionValidUntil(), dto.externalInvoiceId(),
                dto.externalCufe(), dto.computedTotal(), dto.computedTax(), dto.externalTotal(),
                dto.externalTax(), dto.difference(), dto.status(), dto.resolvedBySystemUserId(),
                dto.resolvedAt(), dto.resolutionNote(), dto.postingPeriod(), dto.createdDate());
    }
}
