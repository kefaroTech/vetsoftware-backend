package com.vetsoftware.app.diagnosticimagingtype.application.port.in;

import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListAvailableDiagnosticImagingTypesUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM') or (hasAuthority('diagnosticimaging.read'))")
    List<DiagnosticImagingTypeDto> listAvailable(Long companyId);
}
