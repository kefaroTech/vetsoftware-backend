package com.vetsoftware.app.medicament.application.port.in;

import com.vetsoftware.app.medicament.application.command.UpdateMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateMedicamentUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('prescription.update')")
    MedicamentDto execute(UpdateMedicamentCommand command);
}
