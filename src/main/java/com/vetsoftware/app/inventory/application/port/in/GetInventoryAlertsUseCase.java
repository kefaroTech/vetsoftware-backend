package com.vetsoftware.app.inventory.application.port.in;

import com.vetsoftware.app.inventory.application.command.InventoryAlertsQuery;
import com.vetsoftware.app.inventory.application.dto.InventoryAlertsView;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GetInventoryAlertsUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('inventory.read') and @authz.isMyCompany(#query.companyId))")
  InventoryAlertsView alerts(InventoryAlertsQuery query);
}
