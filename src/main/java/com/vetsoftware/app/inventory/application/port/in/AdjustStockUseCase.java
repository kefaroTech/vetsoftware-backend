package com.vetsoftware.app.inventory.application.port.in;

import com.vetsoftware.app.inventory.application.command.RecordAdjustmentCommand;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AdjustStockUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('inventory.adjust') and @authz.isMyCompany(#command.companyId))")
    void adjust(RecordAdjustmentCommand command);
}
