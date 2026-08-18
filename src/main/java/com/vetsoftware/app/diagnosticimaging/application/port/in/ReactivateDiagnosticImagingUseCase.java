package com.vetsoftware.app.diagnosticimaging.application.port.in;

import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateDiagnosticImagingUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('diagnosticimaging.update')"
            + " and @authz.isMyCompany(#companyId))")
    DiagnosticImagingDto execute(Long id, Long companyId);
}
