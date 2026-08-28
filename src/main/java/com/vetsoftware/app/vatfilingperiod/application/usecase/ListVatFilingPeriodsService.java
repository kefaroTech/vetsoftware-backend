package com.vetsoftware.app.vatfilingperiod.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.vatfilingperiod.application.dto.VatFilingPeriodDto;
import com.vetsoftware.app.vatfilingperiod.application.port.in.ListVatFilingPeriodsUseCase;
import com.vetsoftware.app.vatfilingperiod.application.port.out.VatFilingPeriodRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "vatfiling.list")
@Service
public class ListVatFilingPeriodsService implements ListVatFilingPeriodsUseCase {

    private final VatFilingPeriodRepository repository;

    public ListVatFilingPeriodsService(VatFilingPeriodRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<VatFilingPeriodDto> listAll(Long companyId, int page, int pageSize) {
        return repository.findAll(page, pageSize).map(VatFilingPeriodDto::from);
    }
}
