package com.vetsoftware.app.medicament.application.port.in;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateMedicamentUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('prescription.update')")
    MedicamentDto execute(Long id);
}
