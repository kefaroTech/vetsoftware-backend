package com.vetsoftware.app.paymentreversal.application.dto;

import com.vetsoftware.app.paymentreversal.domain.ConsumerDetermination;
import com.vetsoftware.app.paymentreversal.domain.OppositionGround;
import com.vetsoftware.app.paymentreversal.domain.PaymentReversalRequest;
import com.vetsoftware.app.paymentreversal.domain.ReversalCausal;
import com.vetsoftware.app.paymentreversal.domain.ReversalOrigin;
import com.vetsoftware.app.paymentreversal.domain.ReversalOutcome;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * El expediente tal como sale de la capa de aplicacion.
 *
 * <p>
 * Lleva las <strong>tres fechas</strong> y las dos caras del expediente —la del
 * reclamante y la propia—: sin el lado propio, lo que se expone es media
 * historia y la oposicion deja de ser auditable.
 */
public record PaymentReversalRequestDto(Long id, Long companyId, Long paymentId,
        ReversalOrigin origin, ReversalCausal causal, ConsumerDetermination consumerDetermination,
        LocalDateTime consumerBecameAwareAt, LocalDateTime claimReceivedAt,
        LocalDateTime issuerNotifiedAt, String claimEvidenceRef, String acknowledgementRef,
        LocalDateTime acknowledgedAt, OppositionGround oppositionGround,
        String oppositionEvidenceRef, LocalDateTime opposedAt, LocalDateTime deadlineAt,
        BigDecimal appliedAmount, ReversalOutcome outcome, Long resultingRefundId,
        LocalDateTime createdDate, Long version) {

    public static PaymentReversalRequestDto from(PaymentReversalRequest request) {
        return new PaymentReversalRequestDto(request.getId(), request.getCompanyId(),
                request.getPaymentId(), request.getOrigin(), request.getCausal(),
                request.getConsumerDetermination(), request.getConsumerBecameAwareAt(),
                request.getClaimReceivedAt(), request.getIssuerNotifiedAt(),
                request.getClaimEvidenceRef(), request.getAcknowledgementRef(),
                request.getAcknowledgedAt(), request.getOppositionGround(),
                request.getOppositionEvidenceRef(), request.getOpposedAt(), request.getDeadlineAt(),
                request.getAppliedAmount(), request.getOutcome(), request.getResultingRefundId(),
                request.getCreatedDate(), request.getVersion());
    }
}
