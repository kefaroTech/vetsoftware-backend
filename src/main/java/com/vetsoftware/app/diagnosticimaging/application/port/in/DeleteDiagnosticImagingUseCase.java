package com.vetsoftware.app.diagnosticimaging.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteDiagnosticImagingUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('diagnosticimaging.delete') or hasRole('SYSTEM')")
    void execute(Long id);
}
