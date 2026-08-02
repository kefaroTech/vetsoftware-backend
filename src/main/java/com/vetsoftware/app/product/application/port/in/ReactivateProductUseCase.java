package com.vetsoftware.app.product.application.port.in;

import com.vetsoftware.app.product.application.dto.ProductDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateProductUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('product.delete') and @authz.isMyCompany(#companyId))")
    ProductDto execute(Long id, Long companyId);
}
