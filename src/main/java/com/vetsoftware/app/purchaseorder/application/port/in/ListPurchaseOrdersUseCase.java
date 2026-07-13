package com.vetsoftware.app.purchaseorder.application.port.in;

import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListPurchaseOrdersUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('purchaseOrder.read') and @authz.isMyCompany(#companyId))")
    List<PurchaseOrderDto> listByCompany(Long companyId);

    /** Lista las órdenes de compra PAUSADAS (enabled=false) de la empresa, para el flujo de reactivación. */
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('purchaseOrder.read') and @authz.isMyCompany(#companyId))")
    List<PurchaseOrderDto> listDisabledByCompany(Long companyId);
}
