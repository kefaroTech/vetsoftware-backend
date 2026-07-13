package com.vetsoftware.app.inventory.application.port.in;

import com.vetsoftware.app.inventory.application.command.SearchCountsQuery;
import com.vetsoftware.app.inventory.application.dto.InventoryCountView;
import com.vetsoftware.app.inventory.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/** Historial de sesiones de conteo por sede (resumen, sin líneas). Gate: lectura de inventario. */
public interface ListCountsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('inventory.read') and @authz.isMyCompany(#query.companyId))")
    PageResult<InventoryCountView> list(SearchCountsQuery query);
}
