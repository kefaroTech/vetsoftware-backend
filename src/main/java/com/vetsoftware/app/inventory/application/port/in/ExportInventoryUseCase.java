package com.vetsoftware.app.inventory.application.port.in;

import com.vetsoftware.app.inventory.application.command.SearchKardexCommand;
import com.vetsoftware.app.inventory.application.command.SearchPurchasesQuery;
import com.vetsoftware.app.inventory.application.dto.KardexReport;
import com.vetsoftware.app.inventory.application.dto.PurchasesReport;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Construye los modelos de reporte (kardex + libro de compras) para exportar a CSV/PDF. Gate:
 * lectura de inventario.
 */
public interface ExportInventoryUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('inventory.read') and @authz.isMyCompany(#command.companyId))")
  KardexReport kardexReport(SearchKardexCommand command);

  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('inventory.read') and @authz.isMyCompany(#query.companyId))")
  PurchasesReport purchasesReport(SearchPurchasesQuery query);
}
