package com.vetsoftware.app.supplier.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteSupplierUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('supplier.delete') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
