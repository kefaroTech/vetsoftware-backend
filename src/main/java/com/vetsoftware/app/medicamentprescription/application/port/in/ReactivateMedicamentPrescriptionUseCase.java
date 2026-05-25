package com.vetsoftware.app.medicamentprescription.application.port.in;

import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateMedicamentPrescriptionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('medicamentprescription.update') or hasRole('SYSTEM')")
    MedicamentPrescriptionDto execute(Long id);
}
