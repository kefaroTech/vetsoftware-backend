package com.vetsoftware.app.cashregister.application.port.in;

import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import org.springframework.security.access.prepost.PreAuthorize;

/** Sesión OPEN de una sede (con totales por método), o null si no hay caja abierta. Gate: leer caja. */
public interface GetCurrentCashSessionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('cashregister.read') and @authz.isMyCompany(#companyId))")
    CashSessionView current(Long companyId, Long branchId, String terminal);
}
