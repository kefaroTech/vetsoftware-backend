package com.vetsoftware.app.paymentreversal.infrastructure.web.response;

import com.vetsoftware.app.paymentreversal.application.dto.PaymentReversalRequestDto;
import com.vetsoftware.app.paymentreversal.domain.ConsumerDetermination;
import com.vetsoftware.app.paymentreversal.domain.OppositionGround;
import com.vetsoftware.app.paymentreversal.domain.ReversalCausal;
import com.vetsoftware.app.paymentreversal.domain.ReversalOrigin;
import com.vetsoftware.app.paymentreversal.domain.ReversalOutcome;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * El expediente tal como sale por HTTP.
 *
 * <p>
 * <strong>Aqui no hay nada que redactar.</strong> La regla del bloque «Cobro y
 * saldos» —nunca el codigo de rechazo crudo de la pasarela, solo su clase—
 * apunta a {@code payment_attempts.gateway_decline_code}, y esta tabla no tiene
 * ninguna columna equivalente: {@code causal} y {@code opposition_ground} son
 * listas cerradas propias, no codigos de un tercero que envejezcan.
 *
 * <p>
 * Las <strong>tres fechas</strong> viajan enteras, que es la mitad del valor
 * del expediente: sin ellas el cliente no puede comprobar que su reclamacion se
 * atendio dentro de plazo, ni la plataforma alegar que llego fuera.
 */
public record PaymentReversalRequestResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long paymentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ReversalOrigin origin,
        ReversalCausal causal,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ConsumerDetermination consumerDetermination,
        LocalDateTime consumerBecameAwareAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime claimReceivedAt,
        LocalDateTime issuerNotifiedAt, String claimEvidenceRef, String acknowledgementRef,
        LocalDateTime acknowledgedAt, OppositionGround oppositionGround,
        String oppositionEvidenceRef, LocalDateTime opposedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime deadlineAt,
        BigDecimal appliedAmount, ReversalOutcome outcome, Long resultingRefundId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        Long version) {

    public static PaymentReversalRequestResponse from(PaymentReversalRequestDto dto) {
        return new PaymentReversalRequestResponse(dto.id(), dto.companyId(), dto.paymentId(),
                dto.origin(), dto.causal(), dto.consumerDetermination(),
                dto.consumerBecameAwareAt(), dto.claimReceivedAt(), dto.issuerNotifiedAt(),
                dto.claimEvidenceRef(), dto.acknowledgementRef(), dto.acknowledgedAt(),
                dto.oppositionGround(), dto.oppositionEvidenceRef(), dto.opposedAt(),
                dto.deadlineAt(), dto.appliedAmount(), dto.outcome(), dto.resultingRefundId(),
                dto.createdDate(), dto.version());
    }
}
