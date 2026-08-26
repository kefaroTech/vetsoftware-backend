package com.vetsoftware.app.medicament.application.port.in;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Reactiva un medicamento GLOBAL pausado. Ver
 * {@link CreateGlobalMedicamentUseCase}.
 */
public interface ReactivateGlobalMedicamentUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    MedicamentDto execute(Long id);
}
