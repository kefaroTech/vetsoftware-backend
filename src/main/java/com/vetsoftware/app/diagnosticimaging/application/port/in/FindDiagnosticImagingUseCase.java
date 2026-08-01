package com.vetsoftware.app.diagnosticimaging.application.port.in;

import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindDiagnosticImagingUseCase {
    @PreAuthorize("hasRole('SYSTEM') or ((hasAuthority('admin.all') or hasAuthority('diagnosticimaging.read')) and @authz.isMyCompany(#companyId))")
    DiagnosticImagingDto findById(Long id, Long companyId);
}
