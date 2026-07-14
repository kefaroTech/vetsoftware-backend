package com.vetsoftware.app.cashregister.application.port.in;

import com.vetsoftware.app.cashregister.application.dto.CashArqueoReport;
import org.springframework.security.access.prepost.PreAuthorize;

/** Arma el reporte de arqueo de una sesión (para export CSV/PDF). Gate: ver histórico de cajas. */
public interface ExportArqueoUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('cashregister.read') and hasAuthority('cashregister.history.read') "
        + "and @authz.isMyCompany(#companyId))")
    CashArqueoReport arqueo(Long companyId, Long sessionId);
}
