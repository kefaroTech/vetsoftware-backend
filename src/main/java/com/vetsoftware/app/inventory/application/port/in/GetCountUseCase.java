package com.vetsoftware.app.inventory.application.port.in;

import com.vetsoftware.app.inventory.application.dto.InventoryCountView;
import org.springframework.security.access.prepost.PreAuthorize;

/** Detalle de una sesión de conteo (con sus líneas y diferencias). Gate: lectura de inventario. */
public interface GetCountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('inventory.read') and @authz.isMyCompany(#companyId))")
    InventoryCountView get(Long companyId, Long id);
}
