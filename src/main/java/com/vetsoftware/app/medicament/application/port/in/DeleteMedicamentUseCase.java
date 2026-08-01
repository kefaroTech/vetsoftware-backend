package com.vetsoftware.app.medicament.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteMedicamentUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('prescription.delete')")
    void execute(Long id);
}
