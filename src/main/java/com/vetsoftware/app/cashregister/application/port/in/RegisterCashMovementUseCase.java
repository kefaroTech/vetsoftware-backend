package com.vetsoftware.app.cashregister.application.port.in;

import com.vetsoftware.app.cashregister.application.command.RegisterCashMovementCommand;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import org.springframework.security.access.prepost.PreAuthorize;

/** Registrar un movimiento manual (ingreso/retiro/gasto) en una sesión de caja abierta. Gate: operar caja. */
public interface RegisterCashMovementUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('cashregister.operate') and @authz.isMyCompany(#command.companyId))")
    CashSessionView register(RegisterCashMovementCommand command);
}
