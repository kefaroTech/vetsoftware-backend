package com.vetsoftware.app.purchaseorder.application.port.in;

import com.vetsoftware.app.purchaseorder.application.command.UpdatePurchaseOrderCommand;
import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdatePurchaseOrderUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('purchaseOrder.update') and @authz.isMyCompany(#command.companyId))")
    PurchaseOrderDto execute(UpdatePurchaseOrderCommand command);
}
