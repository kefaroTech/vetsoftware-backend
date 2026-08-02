package com.vetsoftware.app.product.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteProductUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('product.delete') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
