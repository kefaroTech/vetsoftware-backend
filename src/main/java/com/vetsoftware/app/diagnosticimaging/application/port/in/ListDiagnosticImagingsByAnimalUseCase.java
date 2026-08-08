package com.vetsoftware.app.diagnosticimaging.application.port.in;

import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import com.vetsoftware.app.diagnosticimaging.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDiagnosticImagingsByAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('diagnosticimaging.create')")
    PageResult<DiagnosticImagingDto> listByAnimal(Long animalId, int page, int pageSize);
}
