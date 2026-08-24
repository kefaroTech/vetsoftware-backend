package com.vetsoftware.app.dunning.infrastructure.persistence;

import com.vetsoftware.app.dunning.application.port.out.DunningEventRepository;
import com.vetsoftware.app.dunning.domain.DunningEvent;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionJpaEntity;
import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionJpaRepository;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaEntity;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaRepository;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDunningEventRepository implements DunningEventRepository {

    private final DunningEventJpaRepository jpaRepository;
    private final DunningEventJpaMapper mapper;
    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final SubscriptionBillingDocumentJpaRepository billingDocumentJpaRepository;

    public JpaDunningEventRepository(DunningEventJpaRepository jpaRepository,
            DunningEventJpaMapper mapper, SubscriptionJpaRepository subscriptionJpaRepository,
            SubscriptionBillingDocumentJpaRepository billingDocumentJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.subscriptionJpaRepository = subscriptionJpaRepository;
        this.billingDocumentJpaRepository = billingDocumentJpaRepository;
    }

    @Override
    public DunningEvent save(DunningEvent event) {
        SubscriptionJpaEntity subscription = subscriptionJpaRepository
                .getReferenceById(event.getSubscription().id());
        SubscriptionBillingDocumentJpaEntity billingDocument = event.getBillingDocument() == null
                ? null
                : billingDocumentJpaRepository.getReferenceById(event.getBillingDocument().id());
        DunningEventJpaEntity saved = jpaRepository
                .save(mapper.toJpa(event, subscription, billingDocument));
        return mapper.toDomain(saved, event.getSubscription(), event.getBillingDocument());
    }

    @Override
    public Optional<DunningEvent> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    /**
     * Orden total: el expediente se lee cronologicamente y el {@code id} desempata
     * dentro del mismo microsegundo, que es lo que evita que dos paginas
     * consecutivas repitan u omitan un evento.
     */
    @Override
    public PageResult<DunningEvent> findAllBySubscriptionIdAndCompanyId(Long subscriptionId,
            Long companyId, int page, int pageSize) {
        Sort order = Sort.by(Sort.Direction.ASC, "occurredAt")
                .and(Sort.by(Sort.Direction.ASC, "id"));
        return Pages.result(jpaRepository.findAllBySubscription_IdAndCompanyId(subscriptionId,
                companyId, Pages.request(page, pageSize, order)), mapper::toDomain);
    }

    @Override
    public PageResult<DunningEvent> findAllByCompanyId(Long companyId, int page, int pageSize) {
        return Pages.result(jpaRepository.findAllByCompanyId(companyId,
                Pages.request(page, pageSize, newestFirst())), mapper::toDomain);
    }

    @Override
    public PageResult<DunningEvent> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, newestFirst())),
                mapper::toDomain);
    }

    private Sort newestFirst() {
        return Sort.by(Sort.Direction.DESC, "occurredAt").and(Sort.by(Sort.Direction.DESC, "id"));
    }
}
