package com.vetsoftware.app.inventory.application.port.in;

import com.vetsoftware.app.inventory.application.command.RecordClinicalUseCommand;
import org.springframework.security.access.prepost.PreAuthorize;

/** Consumo clínico manual de un producto desde una sede (registro directo por inventario). */
public interface ConsumeStockUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('inventory.adjust') and @authz.isMyCompany(#command.companyId))")
  void consume(RecordClinicalUseCommand command);
}
