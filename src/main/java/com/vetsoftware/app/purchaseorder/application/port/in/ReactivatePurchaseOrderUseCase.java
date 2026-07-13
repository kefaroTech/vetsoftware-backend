package com.vetsoftware.app.purchaseorder.application.port.in;

import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivatePurchaseOrderUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('purchaseOrder.delete') and @authz.isMyCompany(#companyId))")
    PurchaseOrderDto execute(Long id, Long companyId);
}
