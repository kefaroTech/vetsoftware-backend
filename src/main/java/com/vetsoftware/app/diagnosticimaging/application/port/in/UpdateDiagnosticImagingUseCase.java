package com.vetsoftware.app.diagnosticimaging.application.port.in;

import com.vetsoftware.app.diagnosticimaging.application.command.UpdateDiagnosticImagingCommand;
import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateDiagnosticImagingUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('diagnosticimaging.update') or hasRole('SYSTEM')")
    DiagnosticImagingDto execute(UpdateDiagnosticImagingCommand command);
}
