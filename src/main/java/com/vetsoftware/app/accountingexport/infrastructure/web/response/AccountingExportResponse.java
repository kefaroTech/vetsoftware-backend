package com.vetsoftware.app.accountingexport.infrastructure.web.response;

import com.vetsoftware.app.accountingexport.application.dto.AccountingExportDto;
import com.vetsoftware.app.accountingexport.domain.AccountingExportKind;
import com.vetsoftware.app.accountingexport.domain.AccountingExportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * La exportacion tal como sale por HTTP. Solo la ve la consola de plataforma.
 *
 * <p>
 * <strong>No lleva {@code version}</strong> ni {@code current_export_marker}:
 * la primera es una barandilla del que escribe y la segunda un centinela del
 * motor que existe para que {@code uq_accounting_exports_current} pueda
 * restringir lo que con {@code NULL} no restringia. Publicarla invitaria a
 * construir logica sobre un detalle de la base.
 */
public record AccountingExportResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String periodKey,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AccountingExportKind exportKind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "1 el primero; sube al rehacer un fichero rechazado.") int attemptNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AccountingExportStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime generatedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long generatedBySystemUserId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal totalDebit,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal totalCredit,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "SHA-256 del contenido del fichero.") String totalsHash,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fileRef,
        @Schema(description = "Presente solo si el contador ya lo recibio.") LocalDateTime deliveredAt,
        @Schema(description = "Presente solo si el contador lo devolvio.") LocalDateTime rejectedAt,
        @Schema(description = "Acompaña siempre a rejectedAt.") String rejectionReason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static AccountingExportResponse from(AccountingExportDto dto) {
        return new AccountingExportResponse(dto.id(), dto.periodKey(), dto.exportKind(),
                dto.attemptNumber(), dto.status(), dto.generatedAt(), dto.generatedBySystemUserId(),
                dto.totalDebit(), dto.totalCredit(), dto.totalsHash(), dto.fileRef(),
                dto.deliveredAt(), dto.rejectedAt(), dto.rejectionReason(), dto.createdDate());
    }
}
