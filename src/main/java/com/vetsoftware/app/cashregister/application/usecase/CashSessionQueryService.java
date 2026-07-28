package com.vetsoftware.app.cashregister.application.usecase;

import com.vetsoftware.app.cashregister.application.command.SearchCashSessionsQuery;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.application.dto.PageResult;
import com.vetsoftware.app.cashregister.application.port.in.GetCashSessionUseCase;
import com.vetsoftware.app.cashregister.application.port.in.GetCurrentCashSessionUseCase;
import com.vetsoftware.app.cashregister.application.port.in.ListCashSessionsUseCase;
import com.vetsoftware.app.cashregister.application.port.in.ListOpenCashSessionsUseCase;
import com.vetsoftware.app.cashregister.application.port.out.CashSessionRepository;
import com.vetsoftware.app.cashregister.domain.CashSessionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lecturas de caja: sesión actual (OPEN) de una sede, detalle por id e historial paginado. */
@Service
public class CashSessionQueryService
        implements GetCurrentCashSessionUseCase, GetCashSessionUseCase, ListCashSessionsUseCase,
        ListOpenCashSessionsUseCase {

    private final CashSessionRepository repository;

    public CashSessionQueryService(CashSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Observed(name = "cash.register.current.session")
    @Transactional(readOnly = true)
    public CashSessionView current(Long companyId, Long employeeId) {
        return repository.findOpenByEmployee(companyId, employeeId).map(CashSessionView::from).orElse(null);
    }

    @Override
    @Observed(name = "cash.register.get.session")
    @Transactional(readOnly = true)
    public CashSessionView get(Long companyId, Long id) {
        return repository.findByIdAndCompany(id, companyId).map(CashSessionView::from)
            .orElseThrow(() -> new CashSessionNotFoundException(id));
    }

    @Override
    @Observed(name = "cash.register.list.sessions")
    @Transactional(readOnly = true)
    public PageResult<CashSessionView> list(SearchCashSessionsQuery query) {
        return repository.search(query);
    }

    @Override
    @Observed(name = "cash.register.list.open.sessions")
    @Transactional(readOnly = true)
    public List<CashSessionView> listOpen(Long companyId, Set<Long> accessibleBranchIds) {
        return repository.findOpenSummaries(companyId, accessibleBranchIds);
    }
}
