package com.vetsoftware.app.diagnosticimaging.application.port.in;

import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateDiagnosticImagingUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('diagnosticimaging.update') or hasRole('SYSTEM')")
    DiagnosticImagingDto execute(Long id);
}
