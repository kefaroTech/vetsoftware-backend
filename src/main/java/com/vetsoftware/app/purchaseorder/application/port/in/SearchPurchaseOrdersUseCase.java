package com.vetsoftware.app.purchaseorder.application.port.in;

import com.vetsoftware.app.purchaseorder.application.command.SearchPurchaseOrdersCommand;
import com.vetsoftware.app.purchaseorder.application.dto.PageResult;
import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SearchPurchaseOrdersUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('purchaseOrder.read') and @authz.isMyCompany(#command.companyId))")
  PageResult<PurchaseOrderDto> execute(SearchPurchaseOrdersCommand command);
}
