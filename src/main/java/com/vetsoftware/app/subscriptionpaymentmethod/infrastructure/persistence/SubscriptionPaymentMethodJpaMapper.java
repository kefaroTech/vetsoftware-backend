package com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.persistence;

import com.vetsoftware.app.subscriptionpaymentmethod.domain.SubscriptionPaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionPaymentMethodJpaMapper {

    public SubscriptionPaymentMethodJpaEntity toJpa(SubscriptionPaymentMethod paymentMethod) {
        SubscriptionPaymentMethodJpaEntity entity = new SubscriptionPaymentMethodJpaEntity();
        entity.setId(paymentMethod.getId());
        entity.setCompanyId(paymentMethod.getCompanyId());
        entity.setMethodKind(paymentMethod.getMethodKind());
        entity.setGateway(paymentMethod.getGateway());
        entity.setToken(paymentMethod.getToken());
        entity.setBrand(paymentMethod.getBrand());
        entity.setLastFour(paymentMethod.getLastFour());
        entity.setExpiresOn(paymentMethod.getExpiresOn());
        entity.setMandateStatus(paymentMethod.getMandateStatus());
        entity.setMandateEvidence(paymentMethod.getMandateEvidence());
        entity.setAuthorizedAt(paymentMethod.getAuthorizedAt());
        entity.setRevokedAt(paymentMethod.getRevokedAt());
        entity.setRevokedReason(paymentMethod.getRevokedReason());
        entity.setDefaultMethod(paymentMethod.isDefaultMethod());
        entity.setCreatedDate(paymentMethod.getCreatedDate());
        entity.setEnabled(paymentMethod.isEnabled());
        entity.setVersion(paymentMethod.getVersion());
        return entity;
    }

    public SubscriptionPaymentMethod toDomain(SubscriptionPaymentMethodJpaEntity entity) {
        return new SubscriptionPaymentMethod(entity.getId(), entity.getCompanyId(),
                entity.getMethodKind(), entity.getGateway(), entity.getToken(), entity.getBrand(),
                entity.getLastFour(), entity.getExpiresOn(), entity.getMandateStatus(),
                entity.getMandateEvidence(), entity.getAuthorizedAt(), entity.getRevokedAt(),
                entity.getRevokedReason(), entity.isDefaultMethod(), entity.getCreatedDate(),
                entity.isEnabled(), entity.getVersion());
    }
}
