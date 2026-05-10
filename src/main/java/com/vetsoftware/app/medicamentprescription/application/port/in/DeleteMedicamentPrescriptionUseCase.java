package com.vetsoftware.app.medicamentprescription.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteMedicamentPrescriptionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
