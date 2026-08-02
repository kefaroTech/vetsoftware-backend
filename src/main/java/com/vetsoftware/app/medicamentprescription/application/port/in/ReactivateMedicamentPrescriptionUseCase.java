package com.vetsoftware.app.medicamentprescription.application.port.in;

import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateMedicamentPrescriptionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('medicamentPrescription.update')")
    MedicamentPrescriptionDto execute(Long id);
}
