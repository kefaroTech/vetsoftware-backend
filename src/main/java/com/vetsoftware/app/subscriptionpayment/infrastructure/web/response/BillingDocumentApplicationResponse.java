package com.vetsoftware.app.subscriptionpayment.infrastructure.web.response;

import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Una aplicacion tal como viaja por HTTP.
 *
 * <p>
 * <b>Los cinco campos nuevos son opcionales todos, y su ausencia significa algo
 * concreto</b>: cada origen rellena solo el suyo
 * ({@code chk_bda_source_exclusive}), asi que un {@code null} en
 * {@code withholdingId} no es un dato que falte sino la afirmacion de que este
 * abono no salio de una retencion. {@code valueDate} si viene siempre: es la
 * fecha con la que el asiento cae en un periodo, y sin ella no se puede cerrar
 * un mes contable.
 */
public record BillingDocumentApplicationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BillingDocumentSummary targetDocument,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ApplicationSourceKind sourceKind,
        Long paymentId, BillingDocumentSummary sourceDocument,
        @Schema(description = "La retencion practicada, cuando el origen es WITHHOLDING.") Long withholdingId,
        @Schema(description = "El lote de saldo a favor, cuando el origen es CUSTOMER_CREDIT.") Long creditEntryId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal appliedAmount,
        Long reversalOfId,
        @Schema(description = "Firma nominal de plataforma. Solo en WRITE_OFF.") Long writeOffAuthorizedBySystemUserId,
        @Schema(description = "Motivo escrito del castigo. Solo en WRITE_OFF.") String writeOffReason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime appliedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Cuando el asiento cuenta, que no es cuando se registro.") LocalDate valueDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {
}
