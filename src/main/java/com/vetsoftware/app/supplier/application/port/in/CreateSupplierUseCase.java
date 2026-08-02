package com.vetsoftware.app.supplier.application.port.in;

import com.vetsoftware.app.supplier.application.command.CreateSupplierCommand;
import com.vetsoftware.app.supplier.application.dto.SupplierDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateSupplierUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('supplier.create') and @authz.isMyCompany(#command.companyId))")
    SupplierDto execute(CreateSupplierCommand command);
}
