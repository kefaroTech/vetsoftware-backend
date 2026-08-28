package com.vetsoftware.app.vatfilingperiod.infrastructure.persistence;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.vatfilingperiod.application.port.out.VatFilingPeriodRepository;
import com.vetsoftware.app.vatfilingperiod.domain.VatFilingPeriod;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaVatFilingPeriodRepository implements VatFilingPeriodRepository {

    private final VatFilingPeriodJpaRepository jpaRepository;
    private final VatFilingPeriodJpaMapper mapper;

    public JpaVatFilingPeriodRepository(VatFilingPeriodJpaRepository jpaRepository,
            VatFilingPeriodJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public VatFilingPeriod save(VatFilingPeriod period) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(period)));
    }

    @Override
    public Optional<VatFilingPeriod> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<VatFilingPeriod> findByFiscalYear(int fiscalYear) {
        return jpaRepository.findByFiscalYear((short) fiscalYear).map(mapper::toDomain);
    }

    @Override
    public boolean existsByFiscalYear(int fiscalYear) {
        return jpaRepository.existsByFiscalYear((short) fiscalYear);
    }

    @Override
    public PageResult<VatFilingPeriod> findAll(int page, int pageSize) {
        Sort orden = Sort.by(Sort.Direction.DESC, "fiscalYear")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, orden)),
                mapper::toDomain);
    }
}
