package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionPaymentJpaMapper {

    public SubscriptionPaymentJpaEntity toJpa(SubscriptionPayment payment) {
        SubscriptionPaymentJpaEntity entity = new SubscriptionPaymentJpaEntity();
        entity.setId(payment.getId());
        entity.setCompanyId(payment.getCompanyId());
        entity.setAmount(payment.getAmount());
        entity.setCurrency(payment.getCurrency());
        entity.setPaymentMethod(payment.getPaymentMethod());
        entity.setGateway(payment.getGateway());
        entity.setGatewayReference(payment.getGatewayReference());
        entity.setReceivedAt(payment.getReceivedAt());
        entity.setStatus(payment.getStatus());
        entity.setReconciledAt(payment.getReconciledAt());
        entity.setFeeAmount(payment.getFeeAmount());
        entity.setNetAmount(payment.getNetAmount());
        entity.setSettlementReference(payment.getSettlementReference());
        entity.setSettledOn(payment.getSettledOn());
        entity.setRefundedAmount(payment.getRefundedAmount());
        entity.setClientRequestId(payment.getClientRequestId());
        entity.setCreatedDate(payment.getCreatedDate());
        entity.setVersion(payment.getVersion());
        return entity;
    }

    public SubscriptionPayment toDomain(SubscriptionPaymentJpaEntity entity) {
        return new SubscriptionPayment(entity.getId(), entity.getCompanyId(), entity.getAmount(),
                entity.getCurrency(), entity.getPaymentMethod(), entity.getGateway(),
                entity.getGatewayReference(), entity.getReceivedAt(), entity.getStatus(),
                entity.getReconciledAt(), entity.getFeeAmount(), entity.getNetAmount(),
                entity.getSettlementReference(), entity.getSettledOn(), entity.getRefundedAmount(),
                entity.getClientRequestId(), entity.getCreatedDate(), entity.getVersion());
    }
}
