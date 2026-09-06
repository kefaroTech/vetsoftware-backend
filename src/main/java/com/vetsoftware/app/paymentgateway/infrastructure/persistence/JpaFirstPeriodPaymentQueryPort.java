package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import com.vetsoftware.app.paymentgateway.application.port.out.FirstPeriodPaymentQueryPort;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodPaymentSnapshot;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.SubscriptionPaymentJpaEntity;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.SubscriptionPaymentJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Consume {@code SubscriptionPaymentJpaRepository} de
 * {@code subscriptionpayment} por su variante acotada por empresa, sin importar
 * su dominio: el estado viaja como {@code name()} del enum ajeno.
 */
@Component
public class JpaFirstPeriodPaymentQueryPort implements FirstPeriodPaymentQueryPort {

    private final SubscriptionPaymentJpaRepository subscriptionPaymentJpaRepository;

    public JpaFirstPeriodPaymentQueryPort(
            SubscriptionPaymentJpaRepository subscriptionPaymentJpaRepository) {
        this.subscriptionPaymentJpaRepository = subscriptionPaymentJpaRepository;
    }

    @Override
    public Optional<FirstPeriodPaymentSnapshot> findByCompanyIdAndReference(Long companyId,
            String reference) {
        return subscriptionPaymentJpaRepository
                .findByCompanyIdAndClientRequestId(companyId, reference).map(this::toSnapshot);
    }

    @Override
    public Optional<FirstPeriodPaymentSnapshot> findByGatewayAndReference(String gateway,
            String gatewayReference) {
        return subscriptionPaymentJpaRepository
                .findByGatewayAndGatewayReference(gateway, gatewayReference).map(this::toSnapshot);
    }

    private FirstPeriodPaymentSnapshot toSnapshot(SubscriptionPaymentJpaEntity p) {
        return new FirstPeriodPaymentSnapshot(p.getId(), p.getCompanyId(), p.getStatus().name(),
                p.getAmount(), p.getCurrency(), p.getGatewayReference(), p.getReceivedAt());
    }
}
