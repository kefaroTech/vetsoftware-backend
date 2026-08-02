package com.vetsoftware.app.inventory.application.port.in;

import com.vetsoftware.app.inventory.application.command.InventoryValuationQuery;
import com.vetsoftware.app.inventory.application.dto.InventoryValuationView;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GetInventoryValuationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('inventory.read') and @authz.isMyCompany(#query.companyId))")
    InventoryValuationView valuation(InventoryValuationQuery query);
}
