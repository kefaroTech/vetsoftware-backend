package com.vetsoftware.app.medicament.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteMedicamentUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('prescription.delete') or hasRole('SYSTEM')")
    void execute(Long id);
}
