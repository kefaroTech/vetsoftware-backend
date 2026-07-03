package com.vetsoftware.app.diagnosticimagingtype.application.port.in;

import com.vetsoftware.app.diagnosticimagingtype.application.command.CreateDiagnosticImagingTypeCommand;
import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateDiagnosticImagingTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM') or (hasAuthority('diagnosticimaging.create') and @authz.isMyCompany(#command.companyId))")
    DiagnosticImagingTypeDto execute(CreateDiagnosticImagingTypeCommand command);
}
