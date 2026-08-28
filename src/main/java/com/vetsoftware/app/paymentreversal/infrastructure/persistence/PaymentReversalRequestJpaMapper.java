package com.vetsoftware.app.paymentreversal.infrastructure.persistence;

import com.vetsoftware.app.paymentreversal.domain.PaymentReversalRequest;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * Un solo {@code toDomain}, sin el segundo overload del patron cross-feature:
 * esta entidad no tiene asociaciones que hidratar, asi que no hay proxy que
 * evitar ni {@code Ref} precargado que reusar.
 */
@Component
public class PaymentReversalRequestJpaMapper {

    public PaymentReversalRequestJpaEntity toJpa(PaymentReversalRequest request) {
        PaymentReversalRequestJpaEntity entity = new PaymentReversalRequestJpaEntity();
        entity.setId(request.getId());
        entity.setCompanyId(request.getCompanyId());
        entity.setPaymentId(request.getPaymentId());
        entity.setOrigin(request.getOrigin());
        entity.setCausal(request.getCausal());
        entity.setConsumerDetermination(request.getConsumerDetermination());
        entity.setConsumerBecameAwareAt(request.getConsumerBecameAwareAt());
        entity.setClaimReceivedAt(request.getClaimReceivedAt());
        entity.setIssuerNotifiedAt(request.getIssuerNotifiedAt());
        entity.setClaimEvidenceRef(request.getClaimEvidenceRef());
        entity.setAcknowledgementRef(request.getAcknowledgementRef());
        entity.setAcknowledgedAt(request.getAcknowledgedAt());
        entity.setOppositionGround(request.getOppositionGround());
        entity.setOppositionEvidenceRef(request.getOppositionEvidenceRef());
        entity.setOpposedAt(request.getOpposedAt());
        entity.setDeadlineAt(request.getDeadlineAt());
        entity.setAppliedAmount(request.getAppliedAmount());
        entity.setOutcome(request.getOutcome());
        entity.setResultingRefundId(request.getResultingRefundId());
        entity.setCreatedDate(request.getCreatedDate());
        entity.setVersion(request.getVersion());
        return entity;
    }

    public PaymentReversalRequest toDomain(PaymentReversalRequestJpaEntity entity) {
        return new PaymentReversalRequest(entity.getId(), entity.getCompanyId(),
                entity.getPaymentId(), entity.getOrigin(), entity.getCausal(),
                entity.getConsumerDetermination(), entity.getConsumerBecameAwareAt(),
                entity.getClaimReceivedAt(), entity.getIssuerNotifiedAt(),
                entity.getClaimEvidenceRef(), entity.getAcknowledgementRef(),
                entity.getAcknowledgedAt(), entity.getOppositionGround(),
                entity.getOppositionEvidenceRef(), entity.getOpposedAt(), entity.getDeadlineAt(),
                entity.getAppliedAmount(), entity.getOutcome(), entity.getResultingRefundId(),
                entity.getCreatedDate(), entity.getVersion());
    }
}
