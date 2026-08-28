package com.vetsoftware.app.companylimitevent.infrastructure.persistence;

import com.vetsoftware.app.companylimitevent.application.port.out.CompanyLimitEventRepository;
import com.vetsoftware.app.companylimitevent.domain.CompanyLimitEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Adaptador de salida de la bitácora de cupo. Solo agrega. */
@Repository
public class JpaCompanyLimitEventRepository implements CompanyLimitEventRepository {

    private final CompanyLimitEventJpaRepository jpaRepository;
    private final CompanyLimitEventJpaMapper mapper;

    public JpaCompanyLimitEventRepository(CompanyLimitEventJpaRepository jpaRepository,
            CompanyLimitEventJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CompanyLimitEvent append(CompanyLimitEvent event) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(event)));
    }

    @Override
    public List<CompanyLimitEvent> findAllByCompanyIdBetween(Long companyId, LocalDateTime from,
            LocalDateTime to) {
        return jpaRepository
                .findAllByCompanyIdAndOccurredAtBetweenOrderByOccurredAtAscIdAsc(companyId, from,
                        to)
                .stream().map(mapper::toDomain).toList();
    }
}
