package com.vetsoftware.app.company.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteCompanyUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    void execute(Long id);
}
