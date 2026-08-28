package com.vetsoftware.app.paymentrefund.infrastructure.web.response;

import com.vetsoftware.app.paymentrefund.application.dto.PaymentRefundDto;
import com.vetsoftware.app.paymentrefund.domain.RefundMethod;
import com.vetsoftware.app.paymentrefund.domain.RefundReasonCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lo que ve <strong>la plataforma</strong> de la misma devolucion.
 *
 * <p>
 * <strong>Este si publica {@code authorizedBySystemUserId}</strong>, y es el
 * motivo de que existan dos records en vez de uno. Ese campo es el id interno
 * del operador de VetSoftware que autorizo la salida de caja: entero pequeño y
 * enumerable, asi que servido al tenant permite mapear la plantilla interna y
 * correlacionar que operador atiende a que clinica. Quien opera la tesoreria
 * necesita saber quien firmo cada devolucion; el cliente no.
 *
 * <p>
 * Solo lo sirven rutas bajo {@code /system/**}, cerradas a
 * {@code hasRole('SYSTEM')} a secas.
 *
 * <p>
 * Tampoco este lleva {@code clientRequestId}: la llave de idempotencia es una
 * barandilla del que escribe, no un dato del expediente.
 */
public record SystemPaymentRefundResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long paymentId, Long sourceDocumentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal amount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RefundMethod method,
        String destinationReference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime refundedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate valueDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RefundReasonCode reasonCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Id interno del operador que autorizo la devolucion. Solo plataforma.") Long authorizedBySystemUserId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static SystemPaymentRefundResponse from(PaymentRefundDto dto) {
        return new SystemPaymentRefundResponse(dto.id(), dto.companyId(), dto.paymentId(),
                dto.sourceDocumentId(), dto.amount(), dto.method(), dto.destinationReference(),
                dto.refundedAt(), dto.valueDate(), dto.reasonCode(), dto.reason(),
                dto.authorizedBySystemUserId(), dto.createdDate());
    }
}
