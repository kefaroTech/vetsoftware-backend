package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeStatus;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/** Adaptador de {@link SubscriptionChargeRepository}. */
@Repository
public class JpaSubscriptionChargeRepository implements SubscriptionChargeRepository {

    private final SubscriptionChargeJpaRepository jpaRepository;
    private final SubscriptionChargeJpaMapper mapper;

    public JpaSubscriptionChargeRepository(SubscriptionChargeJpaRepository jpaRepository,
            SubscriptionChargeJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public SubscriptionCharge save(SubscriptionCharge charge) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(charge)));
    }

    @Override
    public Optional<SubscriptionCharge> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<SubscriptionCharge> findAllByIdsAndCompanyId(List<Long> ids, Long companyId) {
        if (ids == null || ids.isEmpty())
            return List.of();
        return jpaRepository.findAllByIdInAndCompanyId(ids, companyId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<SubscriptionCharge> findPendingByCompanyIdAndSubscription(Long companyId,
            Long subscriptionId, LocalDate periodStart, LocalDate periodEnd) {
        return jpaRepository.findPendingForPeriod(companyId, subscriptionId, ChargeStatus.PENDING,
                periodStart, periodEnd).stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<SubscriptionCharge> findAllByCompanyId(Long companyId, Long subscriptionId,
            ChargeStatus status, int page, int pageSize) {
        // Orden total: sin el desempate por id, dos paginas consecutivas del mismo
        // periodo de servicio pueden repetir u omitir cargos.
        Sort order = Sort.by(Sort.Direction.DESC, "servicePeriodStart")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        return Pages.result(jpaRepository.findAllByCompany(companyId, subscriptionId, status,
                Pages.request(page, pageSize, order)), mapper::toDomain);
    }

    @Override
    public int sealAsInvoiced(List<Long> ids, Long companyId, Long billingDocumentId) {
        if (ids == null || ids.isEmpty())
            return 0;
        return jpaRepository.sealAsInvoiced(ids, companyId, billingDocumentId);
    }
}
