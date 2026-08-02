package com.vetsoftware.app.inventory.application.port.in;

import com.vetsoftware.app.inventory.application.command.TransferStockCommand;
import org.springframework.security.access.prepost.PreAuthorize;

public interface TransferStockUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('inventory.transfer') and @authz.isMyCompany(#command.companyId))")
    void transfer(TransferStockCommand command);
}
