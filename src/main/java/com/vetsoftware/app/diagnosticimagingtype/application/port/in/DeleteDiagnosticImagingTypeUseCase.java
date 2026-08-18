package com.vetsoftware.app.diagnosticimagingtype.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteDiagnosticImagingTypeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('diagnosticimaging.delete')"
            + " and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
