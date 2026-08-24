package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionStatusHistoryRepository;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChange;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/** Solo anade y lee. La bitacora no se actualiza ni se borra. */
@Repository
public class JpaSubscriptionStatusHistoryRepository implements SubscriptionStatusHistoryRepository {

    private final SubscriptionStatusHistoryJpaRepository jpaRepository;
    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final SubscriptionStatusHistoryJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaSubscriptionStatusHistoryRepository(
            SubscriptionStatusHistoryJpaRepository jpaRepository,
            SubscriptionJpaRepository subscriptionJpaRepository,
            SubscriptionStatusHistoryJpaMapper mapper, CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.subscriptionJpaRepository = subscriptionJpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public SubscriptionStatusChange append(SubscriptionStatusChange change) {
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(change.getCompanyId());
        SubscriptionJpaEntity subscription = subscriptionJpaRepository
                .getReferenceById(change.getSubscriptionId());
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(change, company, subscription)));
    }

    @Override
    public PageResult<SubscriptionStatusChange> findAllBySubscriptionIdAndCompanyId(
            Long subscriptionId, Long companyId, int page, int pageSize) {
        return Pages.result(jpaRepository.findAllBySubscription_IdAndCompany_Id(subscriptionId,
                companyId, Pages.request(page, pageSize, order())), mapper::toDomain);
    }

    /**
     * Mas reciente primero. El desempate por id importa aqui mas que en ningun otro
     * listado: {@code occurred_at} lleva microsegundos precisamente porque dos
     * transiciones del mismo segundo tienen que ordenarse, y el id cierra el caso
     * en que ni eso alcanza.
     */
    private static Sort order() {
        return Sort.by(Sort.Direction.DESC, "occurredAt").and(Sort.by(Sort.Direction.DESC, "id"));
    }
}
