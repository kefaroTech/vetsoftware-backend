package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import com.vetsoftware.app.paymentgateway.application.port.out.BillingDocumentChargeQueryPort;
import com.vetsoftware.app.paymentgateway.domain.BillingDocumentChargeSnapshot;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaEntity;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Consume {@code SubscriptionBillingDocumentJpaRepository} de
 * {@code subscriptionbilling} por su variante acotada por empresa, sin importar
 * su dominio. {@code currency} viaja fija en {@code "COP"}: el documento de
 * cobro no modela todavía la divisa (ver
 * {@code IssueSubscriptionPeriodDocumentService}).
 */
@Component
public class JpaBillingDocumentChargeQueryPort implements BillingDocumentChargeQueryPort {

    private static final String CURRENCY = "COP";

    private final SubscriptionBillingDocumentJpaRepository repository;

    public JpaBillingDocumentChargeQueryPort(SubscriptionBillingDocumentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<BillingDocumentChargeSnapshot> findByIdAndCompanyId(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(this::toSnapshot);
    }

    private BillingDocumentChargeSnapshot toSnapshot(SubscriptionBillingDocumentJpaEntity entity) {
        return new BillingDocumentChargeSnapshot(entity.getId(), entity.getDocumentNumber(),
                entity.getTotalAmount(), entity.getBalanceAmount(), CURRENCY,
                entity.getSubscriptionId());
    }
}
