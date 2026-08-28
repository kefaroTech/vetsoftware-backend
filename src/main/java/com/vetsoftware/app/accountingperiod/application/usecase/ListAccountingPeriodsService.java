package com.vetsoftware.app.accountingperiod.application.usecase;

import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import com.vetsoftware.app.accountingperiod.application.port.in.ListAccountingPeriodsUseCase;
import com.vetsoftware.app.accountingperiod.application.port.out.AccountingPeriodRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "accounting.period.list")
@Service
public class ListAccountingPeriodsService implements ListAccountingPeriodsUseCase {

    private final AccountingPeriodRepository repository;

    public ListAccountingPeriodsService(AccountingPeriodRepository repository) {
        this.repository = repository;
    }

    /**
     * Los totales son los de la consulta y no se recalculan sobre el contenido ya
     * paginado: {@code PageResult.map} conserva los metadatos intactos.
     */
    @Override
    public PageResult<AccountingPeriodDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(AccountingPeriodDto::from);
    }
}
