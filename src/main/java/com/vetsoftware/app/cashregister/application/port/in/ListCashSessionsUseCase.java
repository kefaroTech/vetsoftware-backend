package com.vetsoftware.app.cashregister.application.port.in;

import com.vetsoftware.app.cashregister.application.command.SearchCashSessionsQuery;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Historial paginado de sesiones de caja por sede (resumen, sin movimientos).
 * Gate: ver histórico de cajas.
 */
public interface ListCashSessionsUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('cashregister.read') and hasAuthority('cashregister.history.read') "
            + "and @authz.isMyCompany(#query.companyId))")
    PageResult<CashSessionView> list(SearchCashSessionsQuery query);
}
