package com.vetsoftware.app.cashregister.application.port.in;

import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import org.springframework.security.access.prepost.PreAuthorize;

/** Detalle de una sesión de caja (movimientos + totales + counts). Gate: leer caja. */
public interface GetCashSessionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('cashregister.read') and @authz.isMyCompany(#companyId))")
    CashSessionView get(Long companyId, Long id);
}
