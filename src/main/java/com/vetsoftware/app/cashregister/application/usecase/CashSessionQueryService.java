package com.vetsoftware.app.cashregister.application.usecase;

import com.vetsoftware.app.cashregister.application.command.SearchCashSessionsQuery;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.application.dto.PageResult;
import com.vetsoftware.app.cashregister.application.port.in.GetCashSessionUseCase;
import com.vetsoftware.app.cashregister.application.port.in.GetCurrentCashSessionUseCase;
import com.vetsoftware.app.cashregister.application.port.in.ListCashSessionsUseCase;
import com.vetsoftware.app.cashregister.application.port.out.CashSessionRepository;
import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.domain.CashSessionNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lecturas de caja: sesión actual (OPEN) de una sede, detalle por id e historial paginado. */
@Service
public class CashSessionQueryService
        implements GetCurrentCashSessionUseCase, GetCashSessionUseCase, ListCashSessionsUseCase {

    private final CashSessionRepository repository;

    public CashSessionQueryService(CashSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public CashSessionView current(Long companyId, Long branchId, String terminal) {
        String resolved = (terminal == null || terminal.isBlank()) ? CashSession.DEFAULT_TERMINAL : terminal.trim();
        return repository.findOpen(companyId, branchId, resolved).map(CashSessionView::from).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public CashSessionView get(Long companyId, Long id) {
        return repository.findByIdAndCompany(id, companyId).map(CashSessionView::from)
            .orElseThrow(() -> new CashSessionNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CashSessionView> list(SearchCashSessionsQuery query) {
        return repository.search(query);
    }
}
