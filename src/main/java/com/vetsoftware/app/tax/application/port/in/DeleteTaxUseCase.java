package com.vetsoftware.app.tax.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteTaxUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('tax.delete') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
