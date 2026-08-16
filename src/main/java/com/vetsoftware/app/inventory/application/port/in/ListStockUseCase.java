package com.vetsoftware.app.inventory.application.port.in;

import com.vetsoftware.app.inventory.application.command.SearchStockCommand;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.inventory.application.dto.StockView;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListStockUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('inventory.read') and @authz.isMyCompany(#command.companyId))")
    PageResult<StockView> search(SearchStockCommand command);
}
