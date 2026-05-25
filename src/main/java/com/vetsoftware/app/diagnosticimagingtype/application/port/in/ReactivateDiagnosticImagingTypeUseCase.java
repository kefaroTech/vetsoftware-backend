package com.vetsoftware.app.diagnosticimagingtype.application.port.in;

import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateDiagnosticImagingTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    DiagnosticImagingTypeDto execute(Long id);
}
