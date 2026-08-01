package com.vetsoftware.app.purchaseorder.application.port.in;

import com.vetsoftware.app.purchaseorder.application.command.CreatePurchaseOrderCommand;
import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreatePurchaseOrderUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('purchaseOrder.create') and @authz.isMyCompany(#command.companyId))")
    PurchaseOrderDto execute(CreatePurchaseOrderCommand command);
}
