package com.vetsoftware.app.prescription.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeletePrescriptionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
