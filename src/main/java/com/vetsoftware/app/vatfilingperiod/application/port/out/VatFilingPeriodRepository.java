package com.vetsoftware.app.vatfilingperiod.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.vatfilingperiod.domain.VatFilingPeriod;
import java.util.Optional;

public interface VatFilingPeriodRepository {

    VatFilingPeriod save(VatFilingPeriod period);

    Optional<VatFilingPeriod> findById(Long id);

    Optional<VatFilingPeriod> findByFiscalYear(int fiscalYear);

    boolean existsByFiscalYear(int fiscalYear);

    PageResult<VatFilingPeriod> findAll(int page, int pageSize);
}
