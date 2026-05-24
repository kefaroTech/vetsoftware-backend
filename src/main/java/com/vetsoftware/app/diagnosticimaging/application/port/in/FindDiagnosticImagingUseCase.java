package com.vetsoftware.app.diagnosticimaging.application.port.in;

import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindDiagnosticImagingUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('diagnosticimaging.read') or hasRole('SYSTEM')")
    DiagnosticImagingDto findById(Long id);
}
