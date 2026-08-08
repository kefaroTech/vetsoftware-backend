package com.vetsoftware.app.diagnosticimagingtype.application.port.in;

import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListAvailableDiagnosticImagingTypesUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('diagnosticimaging.read') and @authz.isMyCompany(#companyId))")
    List<DiagnosticImagingTypeDto> listAvailable(Long companyId);
}
