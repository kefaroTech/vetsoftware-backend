package com.vetsoftware.app.purchaseorder.application.port.in;

import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindPurchaseOrderUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('purchaseOrder.read') and @authz.isMyCompany(#companyId))")
  PurchaseOrderDto findById(Long id, Long companyId);
}
