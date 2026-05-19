package com.vetsoftware.app.diagnosticimaging.application.port.in;

import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDiagnosticImagingsByAnimalUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('diagnosticimaging.create') or hasRole('SYSTEM')")
    List<DiagnosticImagingDto> listByAnimal(Long animalId);
}
