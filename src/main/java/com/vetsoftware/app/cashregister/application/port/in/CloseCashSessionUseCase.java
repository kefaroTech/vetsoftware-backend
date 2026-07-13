package com.vetsoftware.app.cashregister.application.port.in;

import com.vetsoftware.app.cashregister.application.command.CloseCashSessionCommand;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import org.springframework.security.access.prepost.PreAuthorize;

/** Cerrar la caja con el conteo por método (esperado vs contado → diferencia). Gate: cerrar caja (≠ operar). */
public interface CloseCashSessionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('cashregister.close') and @authz.isMyCompany(#command.companyId))")
    CashSessionView close(CloseCashSessionCommand command);
}
