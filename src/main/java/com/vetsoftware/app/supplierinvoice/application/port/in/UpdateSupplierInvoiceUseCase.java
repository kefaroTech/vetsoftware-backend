package com.vetsoftware.app.supplierinvoice.application.port.in;

import com.vetsoftware.app.supplierinvoice.application.command.UpdateSupplierInvoiceCommand;
import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoiceDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateSupplierInvoiceUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('supplierinvoice.update') and @authz.isMyCompany(#command.companyId))")
    SupplierInvoiceDto execute(UpdateSupplierInvoiceCommand command);
}
