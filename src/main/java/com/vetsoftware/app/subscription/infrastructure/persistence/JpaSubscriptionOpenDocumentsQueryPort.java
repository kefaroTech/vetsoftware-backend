package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.subscription.application.port.out.SubscriptionOpenDocumentsQueryPort;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JpaSubscriptionOpenDocumentsQueryPort implements SubscriptionOpenDocumentsQueryPort {

    private final SubscriptionBillingDocumentJpaRepository repository;

    public JpaSubscriptionOpenDocumentsQueryPort(
            SubscriptionBillingDocumentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Long> findOpenDocumentIdsWithBalance(Long companyId, Long subscriptionId) {
        return repository.findOpenIdsBySubscriptionId(subscriptionId, companyId);
    }
}
