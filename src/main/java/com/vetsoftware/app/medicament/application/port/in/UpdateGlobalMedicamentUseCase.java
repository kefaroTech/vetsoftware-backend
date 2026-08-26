package com.vetsoftware.app.medicament.application.port.in;

import com.vetsoftware.app.medicament.application.command.UpdateGlobalMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** Edicion en el catalogo GLOBAL. Ver {@link CreateGlobalMedicamentUseCase}. */
public interface UpdateGlobalMedicamentUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    MedicamentDto execute(UpdateGlobalMedicamentCommand command);
}
