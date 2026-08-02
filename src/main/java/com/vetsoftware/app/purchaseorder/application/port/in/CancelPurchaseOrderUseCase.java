package com.vetsoftware.app.purchaseorder.application.port.in;

import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CancelPurchaseOrderUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('purchaseOrder.update') and @authz.isMyCompany(#companyId))")
  PurchaseOrderDto execute(Long id, Long companyId, Long actorId);
}
