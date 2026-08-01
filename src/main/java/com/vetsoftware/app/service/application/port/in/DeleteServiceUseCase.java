package com.vetsoftware.app.service.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteServiceUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('service.delete') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
