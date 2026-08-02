package com.vetsoftware.app.inventory.application.port.in;

import com.vetsoftware.app.inventory.application.command.RecordCountCommand;
import com.vetsoftware.app.inventory.application.dto.InventoryCountView;
import org.springframework.security.access.prepost.PreAuthorize;

/** Confirmar un conteo físico: reconcilia el saldo generando ajustes por la diferencia. Gate: mismo permiso que ajustar. */
public interface RecordCountUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('inventory.adjust') and @authz.isMyCompany(#command.companyId))")
    InventoryCountView record(RecordCountCommand command);
}
