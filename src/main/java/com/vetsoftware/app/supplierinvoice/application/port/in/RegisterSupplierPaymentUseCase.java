package com.vetsoftware.app.supplierinvoice.application.port.in;

import com.vetsoftware.app.supplierinvoice.application.command.RegisterSupplierPaymentCommand;
import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoiceDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RegisterSupplierPaymentUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('supplierinvoice.update') and @authz.isMyCompany(#command.companyId))")
    SupplierInvoiceDto execute(RegisterSupplierPaymentCommand command);
}
