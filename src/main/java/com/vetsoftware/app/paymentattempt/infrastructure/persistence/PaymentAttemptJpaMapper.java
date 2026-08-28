package com.vetsoftware.app.paymentattempt.infrastructure.persistence;

import com.vetsoftware.app.paymentattempt.domain.PaymentAttempt;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA. Sin
 * overloads de lectura y escritura porque no hay asociaciones que hidratar: las
 * referencias ajenas son ids escalares.
 */
@Component
public class PaymentAttemptJpaMapper {

    public PaymentAttemptJpaEntity toJpa(PaymentAttempt attempt) {
        PaymentAttemptJpaEntity entity = new PaymentAttemptJpaEntity();
        entity.setId(attempt.getId());
        entity.setCompanyId(attempt.getCompanyId());
        entity.setBillingDocumentId(attempt.getBillingDocumentId());
        entity.setPaymentMethodId(attempt.getPaymentMethodId());
        entity.setAttemptNumber(attempt.getAttemptNumber());
        entity.setGateway(attempt.getGateway());
        entity.setRequestedAmount(attempt.getRequestedAmount());
        entity.setGatewayDeclineCode(attempt.getGatewayDeclineCode());
        entity.setDeclineKind(attempt.getDeclineKind());
        entity.setAttemptedAt(attempt.getAttemptedAt());
        entity.setNextAttemptAt(attempt.getNextAttemptAt());
        entity.setCreatedDate(attempt.getCreatedDate());
        entity.setVersion(attempt.getVersion());
        return entity;
    }

    public PaymentAttempt toDomain(PaymentAttemptJpaEntity entity) {
        return new PaymentAttempt(entity.getId(), entity.getCompanyId(),
                entity.getBillingDocumentId(), entity.getPaymentMethodId(),
                entity.getAttemptNumber(), entity.getGateway(), entity.getRequestedAmount(),
                entity.getGatewayDeclineCode(), entity.getDeclineKind(), entity.getAttemptedAt(),
                entity.getNextAttemptAt(), entity.getCreatedDate(), entity.getVersion());
    }
}
