package com.vetsoftware.app.numberingresolution.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteNumberingResolutionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('electronicbilling.delete')"
            + " and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
