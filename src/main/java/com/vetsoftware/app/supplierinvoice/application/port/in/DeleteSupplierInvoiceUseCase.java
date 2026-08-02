package com.vetsoftware.app.supplierinvoice.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteSupplierInvoiceUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('supplierinvoice.delete') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
