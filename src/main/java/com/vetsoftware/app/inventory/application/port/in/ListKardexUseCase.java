package com.vetsoftware.app.inventory.application.port.in;

import com.vetsoftware.app.inventory.application.command.SearchKardexCommand;
import com.vetsoftware.app.inventory.application.dto.PageResult;
import com.vetsoftware.app.inventory.application.dto.StockMovementView;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListKardexUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('inventory.read') and @authz.isMyCompany(#command.companyId))")
    PageResult<StockMovementView> searchKardex(SearchKardexCommand command);
}
