package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.CustomerSnapshot;
import java.util.Optional;

/**
 * Lee el adquiriente (Owner) de una venta de POS cuando NO es consumidor final, y lo traduce al
 * snapshot fiscal de esta feature. Unico punto que conoce la feature owner para la venta directa de
 * POS.
 */
public interface SaleCustomerQueryPort {
  Optional<SaleCustomer> findOwner(Long ownerId, Long companyId);

  /** Snapshot del adquiriente + si es agente retenedor (dispara el calculo de retenciones). */
  record SaleCustomer(CustomerSnapshot snapshot, boolean withholdingAgent) {}
}
