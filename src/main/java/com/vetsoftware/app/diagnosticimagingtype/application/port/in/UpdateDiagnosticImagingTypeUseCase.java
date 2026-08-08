package com.vetsoftware.app.diagnosticimagingtype.application.port.in;

import com.vetsoftware.app.diagnosticimagingtype.application.command.UpdateDiagnosticImagingTypeCommand;
import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateDiagnosticImagingTypeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('diagnosticimaging.update') and @authz.isMyCompany(#command.companyId))")
    DiagnosticImagingTypeDto execute(UpdateDiagnosticImagingTypeCommand command);
}
