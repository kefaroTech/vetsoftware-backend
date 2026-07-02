package com.vetsoftware.app.product.application.port.in;

import com.vetsoftware.app.product.application.dto.ProductDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListProductsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('product.read') and @authz.isMyCompany(#companyId))")
    List<ProductDto> listByCompany(Long companyId);

    /** Lista los productos PAUSADOS (enabled=false) de la empresa, para el flujo de reactivación. */
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('product.read') and @authz.isMyCompany(#companyId))")
    List<ProductDto> listDisabledByCompany(Long companyId);
}
