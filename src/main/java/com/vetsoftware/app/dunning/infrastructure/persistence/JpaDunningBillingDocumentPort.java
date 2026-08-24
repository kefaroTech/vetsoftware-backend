package com.vetsoftware.app.dunning.infrastructure.persistence;

import com.vetsoftware.app.dunning.application.port.out.DunningBillingDocumentPort;
import com.vetsoftware.app.dunning.domain.BillingDocumentRef;
import com.vetsoftware.app.dunning.domain.DunningBillingDocumentSnapshot;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaEntity;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaDunningBillingDocumentPort implements DunningBillingDocumentPort {

    private final SubscriptionBillingDocumentJpaRepository repository;

    public JpaDunningBillingDocumentPort(SubscriptionBillingDocumentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<DunningBillingDocumentSnapshot> lockByIdAndCompanyId(Long documentId,
            Long companyId) {
        return repository.lockByIdAndCompanyId(documentId, companyId).map(this::toSnapshot);
    }

    @Override
    public Optional<DunningBillingDocumentSnapshot> findOldestOverdue(Long subscriptionId,
            Long companyId, LocalDate today) {
        return repository.findOldestOverdue(subscriptionId, companyId, today).map(this::toSnapshot);
    }

    @Override
    public List<DunningBillingDocumentSnapshot> lockOverdueBatchAfter(LocalDate today, long afterId,
            int batchSize) {
        return repository.lockOverdueBatchAfter(today, afterId, batchSize).stream()
                .map(this::toSnapshot).toList();
    }

    private DunningBillingDocumentSnapshot toSnapshot(SubscriptionBillingDocumentJpaEntity entity) {
        BillingDocumentRef document = new BillingDocumentRef(entity.getId(), entity.getCompanyId(),
                entity.getDocumentNumber(), entity.getBalanceAmount());
        return new DunningBillingDocumentSnapshot(document, entity.getSubscriptionId(),
                entity.getDueDate());
    }
}
