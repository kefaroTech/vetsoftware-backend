package com.vetsoftware.app.product.application.port.in;

import com.vetsoftware.app.product.application.dto.ProductDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListProductsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or @authz.isMyCompany(#companyId)")
    List<ProductDto> listByCompany(Long companyId);
}
