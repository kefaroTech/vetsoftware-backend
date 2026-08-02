package com.vetsoftware.app.supplier.application.port.in;

import com.vetsoftware.app.supplier.application.dto.SupplierDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSuppliersUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('supplier.read') and @authz.isMyCompany(#companyId))")
    List<SupplierDto> listByCompany(Long companyId);

    /** Lista los proveedores PAUSADOS (enabled=false) de la empresa, para el flujo de reactivación. */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('supplier.read') and @authz.isMyCompany(#companyId))")
    List<SupplierDto> listDisabledByCompany(Long companyId);
}
