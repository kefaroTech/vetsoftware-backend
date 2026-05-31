package com.vetsoftware.app.hospitalizationmedication.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteHospitalizationMedicationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalization.delete') or hasRole('SYSTEM')")
    void execute(Long id);
}
