package com.vetsoftware.app.inventory.application.port.in;

import com.vetsoftware.app.inventory.application.command.SearchStockCommand;
import com.vetsoftware.app.inventory.application.dto.PageResult;
import com.vetsoftware.app.inventory.application.dto.StockView;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListStockUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('inventory.read') and @authz.isMyCompany(#command.companyId))")
    PageResult<StockView> search(SearchStockCommand command);
}
