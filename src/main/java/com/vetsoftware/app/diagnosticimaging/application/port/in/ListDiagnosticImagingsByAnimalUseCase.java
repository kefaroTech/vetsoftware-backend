package com.vetsoftware.app.diagnosticimaging.application.port.in;

import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDiagnosticImagingsByAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('diagnosticimaging.create') and @authz.isMyCompany(#companyId))")
    PageResult<DiagnosticImagingDto> listByAnimal(Long animalId, Long companyId, String query,
            int page, int pageSize);
}
