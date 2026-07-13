package com.vetsoftware.app.inventory.application.port.in;

import com.vetsoftware.app.inventory.application.command.SearchPurchasesQuery;
import com.vetsoftware.app.inventory.application.dto.PageResult;
import com.vetsoftware.app.inventory.application.dto.PurchaseView;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListPurchasesUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('inventory.read') and @authz.isMyCompany(#query.companyId))")
    PageResult<PurchaseView> purchases(SearchPurchasesQuery query);
}
