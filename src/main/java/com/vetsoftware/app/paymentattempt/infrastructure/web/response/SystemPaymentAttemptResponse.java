package com.vetsoftware.app.paymentattempt.infrastructure.web.response;

import com.vetsoftware.app.paymentattempt.application.dto.PaymentAttemptDto;
import com.vetsoftware.app.paymentattempt.domain.DeclineKind;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lo que ve <strong>la plataforma</strong> del mismo intento.
 *
 * <p>
 * <strong>Este si publica {@code gatewayDeclineCode}</strong>, y es el motivo
 * de que existan dos records en vez de uno: el codigo se guarda crudo, tal cual
 * lo devolvio la pasarela y con comparacion exacta, porque las pasarelas
 * cambian su catalogo y una traduccion hecha hoy envejece. Quien opera la
 * cobranza necesita poder revisar despues si la clase que se le asigno era la
 * correcta; el cliente no, y ensenarselo es lo que el documento maestro
 * prohibe.
 *
 * <p>
 * Solo lo sirven rutas bajo {@code /system/**}, cerradas a
 * {@code hasRole('SYSTEM')} a secas.
 */
public record SystemPaymentAttemptResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long billingDocumentId,
        Long paymentMethodId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer attemptNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String gateway,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal requestedAmount,
        @Schema(description = "Codigo crudo de la pasarela. Solo plataforma.") String gatewayDeclineCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) DeclineKind declineKind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime attemptedAt,
        LocalDateTime nextAttemptAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        Long version) {

    public static SystemPaymentAttemptResponse from(PaymentAttemptDto dto) {
        return new SystemPaymentAttemptResponse(dto.id(), dto.companyId(), dto.billingDocumentId(),
                dto.paymentMethodId(), dto.attemptNumber(), dto.gateway(), dto.requestedAmount(),
                dto.gatewayDeclineCode(), dto.declineKind(), dto.attemptedAt(), dto.nextAttemptAt(),
                dto.createdDate(), dto.version());
    }
}
