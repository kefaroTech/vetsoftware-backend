package com.vetsoftware.app.subscriptionitemlimit.infrastructure.persistence;

import com.vetsoftware.app.subscriptionitemlimit.application.port.out.SubscriptionItemLimitRepository;
import com.vetsoftware.app.subscriptionitemlimit.domain.SubscriptionItemLimit;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptador de salida de los techos congelados. */
@Repository
public class JpaSubscriptionItemLimitRepository implements SubscriptionItemLimitRepository {

    private final SubscriptionItemLimitJpaRepository jpaRepository;
    private final SubscriptionItemLimitJpaMapper mapper;

    public JpaSubscriptionItemLimitRepository(SubscriptionItemLimitJpaRepository jpaRepository,
            SubscriptionItemLimitJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public SubscriptionItemLimit save(SubscriptionItemLimit limit) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(limit)));
    }

    @Override
    public List<SubscriptionItemLimit> saveAll(List<SubscriptionItemLimit> limits) {
        if (limits.isEmpty())
            return List.of();
        return jpaRepository.saveAll(limits.stream().map(mapper::toJpa).toList()).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Optional<SubscriptionItemLimit> findByCompanyIdAndSubscriptionItemIdAndLimitDimensionId(
            Long companyId, Long subscriptionItemId, Long limitDimensionId) {
        return jpaRepository.findByCompanyIdAndSubscriptionItemIdAndLimitDimensionId(companyId,
                subscriptionItemId, limitDimensionId).map(mapper::toDomain);
    }

    @Override
    public List<SubscriptionItemLimit> findAllByCompanyId(Long companyId) {
        return jpaRepository.findAllByCompanyIdOrderByLimitDimensionIdAscIdAsc(companyId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<SubscriptionItemLimit> findAllLiveByCatalogItemIdAndLimitDimensionId(
            Long catalogItemId, Long limitDimensionId) {
        return jpaRepository
                .findAllLiveByCatalogItemIdAndLimitDimensionId(catalogItemId, limitDimensionId)
                .stream().map(mapper::toDomain).toList();
    }
}
