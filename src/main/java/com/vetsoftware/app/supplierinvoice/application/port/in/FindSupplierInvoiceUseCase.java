package com.vetsoftware.app.supplierinvoice.application.port.in;

import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoiceDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindSupplierInvoiceUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('supplierinvoice.read') and @authz.isMyCompany(#companyId))")
    SupplierInvoiceDto findById(Long id, Long companyId);
}
