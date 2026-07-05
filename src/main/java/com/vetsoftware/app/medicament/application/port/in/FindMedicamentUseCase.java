package com.vetsoftware.app.medicament.application.port.in;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindMedicamentUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('prescription.read') or hasRole('SYSTEM')")
    MedicamentDto findById(Long id);
}
