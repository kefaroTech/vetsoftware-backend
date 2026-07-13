package com.vetsoftware.app.cashregister.application.port.in;

import com.vetsoftware.app.cashregister.application.command.OpenCashSessionCommand;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import org.springframework.security.access.prepost.PreAuthorize;

/** Abrir la caja de una sede con la base inicial. Gate: operar caja. */
public interface OpenCashSessionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('cashregister.operate') and @authz.isMyCompany(#command.companyId))")
    CashSessionView open(OpenCashSessionCommand command);
}
