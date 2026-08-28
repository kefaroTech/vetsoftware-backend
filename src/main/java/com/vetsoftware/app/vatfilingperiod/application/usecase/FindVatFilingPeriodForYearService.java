package com.vetsoftware.app.vatfilingperiod.application.usecase;

import com.vetsoftware.app.vatfilingperiod.application.dto.VatFilingPeriodDto;
import com.vetsoftware.app.vatfilingperiod.application.port.in.FindVatFilingPeriodForYearUseCase;
import com.vetsoftware.app.vatfilingperiod.application.port.out.VatFilingPeriodRepository;
import com.vetsoftware.app.vatfilingperiod.domain.VatFilingPeriodNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "vatfiling.find")
@Service
public class FindVatFilingPeriodForYearService implements FindVatFilingPeriodForYearUseCase {

    private final VatFilingPeriodRepository repository;

    public FindVatFilingPeriodForYearService(VatFilingPeriodRepository repository) {
        this.repository = repository;
    }

    @Override
    public VatFilingPeriodDto findByYear(int fiscalYear, Long companyId) {
        return repository.findByFiscalYear(fiscalYear).map(VatFilingPeriodDto::from)
                .orElseThrow(() -> new VatFilingPeriodNotFoundException(fiscalYear));
    }
}
