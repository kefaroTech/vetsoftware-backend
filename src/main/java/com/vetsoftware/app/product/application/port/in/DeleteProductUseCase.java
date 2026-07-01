package com.vetsoftware.app.product.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteProductUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('product.delete') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
