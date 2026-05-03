package com.vetsoftware.app.company.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteCompanyUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('company.delete') or hasRole('SYSTEM')")
    void execute(Long id);
}
