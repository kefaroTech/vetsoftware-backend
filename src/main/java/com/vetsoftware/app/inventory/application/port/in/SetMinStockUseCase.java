package com.vetsoftware.app.inventory.application.port.in;

import com.vetsoftware.app.inventory.application.command.SetMinStockCommand;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SetMinStockUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('inventory.adjust') and @authz.isMyCompany(#command.companyId))")
    void setMinStock(SetMinStockCommand command);
}
