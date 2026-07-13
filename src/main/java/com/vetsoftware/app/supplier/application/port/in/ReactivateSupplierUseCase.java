package com.vetsoftware.app.supplier.application.port.in;

import com.vetsoftware.app.supplier.application.dto.SupplierDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateSupplierUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('supplier.delete') and @authz.isMyCompany(#companyId))")
    SupplierDto execute(Long id, Long companyId);
}
