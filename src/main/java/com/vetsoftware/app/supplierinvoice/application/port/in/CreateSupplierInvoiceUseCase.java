package com.vetsoftware.app.supplierinvoice.application.port.in;

import com.vetsoftware.app.supplierinvoice.application.command.CreateSupplierInvoiceCommand;
import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoiceDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateSupplierInvoiceUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('supplierinvoice.create') and @authz.isMyCompany(#command.companyId))")
    SupplierInvoiceDto execute(CreateSupplierInvoiceCommand command);
}
