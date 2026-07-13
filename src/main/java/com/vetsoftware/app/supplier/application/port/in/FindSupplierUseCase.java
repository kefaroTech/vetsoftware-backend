package com.vetsoftware.app.supplier.application.port.in;

import com.vetsoftware.app.supplier.application.dto.SupplierDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindSupplierUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('supplier.read') and @authz.isMyCompany(#companyId))")
    SupplierDto findById(Long id, Long companyId);
}
