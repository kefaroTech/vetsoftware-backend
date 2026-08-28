package com.vetsoftware.app.vatfilingperiod.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VatFilingPeriodJpaRepository
        extends
            JpaRepository<VatFilingPeriodJpaEntity, Long> {

    Optional<VatFilingPeriodJpaEntity> findByFiscalYear(short fiscalYear);

    boolean existsByFiscalYear(short fiscalYear);
}
