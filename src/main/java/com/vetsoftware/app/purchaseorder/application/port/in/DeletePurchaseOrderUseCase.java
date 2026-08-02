package com.vetsoftware.app.purchaseorder.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeletePurchaseOrderUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('purchaseOrder.delete') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
