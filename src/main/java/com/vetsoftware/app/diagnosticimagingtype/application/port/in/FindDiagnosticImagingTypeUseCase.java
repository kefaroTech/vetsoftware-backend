package com.vetsoftware.app.diagnosticimagingtype.application.port.in;

import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindDiagnosticImagingTypeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or ((hasAuthority('admin.all') or hasAuthority('diagnosticimaging.read')) and @authz.isMyCompany(#companyId))")
    DiagnosticImagingTypeDto findById(Long id, Long companyId);
}
