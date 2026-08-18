package com.vetsoftware.app.diagnosticimagingtype.application.port.in;

import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateDiagnosticImagingTypeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('diagnosticimaging.update')"
            + " and @authz.isMyCompany(#companyId))")
    DiagnosticImagingTypeDto execute(Long id, Long companyId);
}
