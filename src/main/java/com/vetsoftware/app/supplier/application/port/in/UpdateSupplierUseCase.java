package com.vetsoftware.app.supplier.application.port.in;

import com.vetsoftware.app.supplier.application.command.UpdateSupplierCommand;
import com.vetsoftware.app.supplier.application.dto.SupplierDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateSupplierUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('supplier.update') and @authz.isMyCompany(#command.companyId))")
    SupplierDto execute(UpdateSupplierCommand command);
}
