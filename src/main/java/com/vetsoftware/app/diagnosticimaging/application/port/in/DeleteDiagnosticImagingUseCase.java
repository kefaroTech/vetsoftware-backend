package com.vetsoftware.app.diagnosticimaging.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteDiagnosticImagingUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('diagnosticimaging.delete') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
