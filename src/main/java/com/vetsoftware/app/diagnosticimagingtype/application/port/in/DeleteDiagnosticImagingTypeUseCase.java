package com.vetsoftware.app.diagnosticimagingtype.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteDiagnosticImagingTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM') or (hasAuthority('diagnosticimaging.delete'))")
    void execute(Long id);
}
