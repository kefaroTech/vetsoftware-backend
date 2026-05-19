package com.vetsoftware.app.diagnosticimaging.application.port.in;

import com.vetsoftware.app.diagnosticimaging.application.command.ChangeDiagnosticImagingStatusCommand;
import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ChangeDiagnosticImagingStatusUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    DiagnosticImagingDto execute(ChangeDiagnosticImagingStatusCommand command);
}
