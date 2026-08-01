package com.vetsoftware.app.medicament.application.port.in;

import com.vetsoftware.app.medicament.application.command.CreateMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateMedicamentUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('prescription.create')")
    MedicamentDto execute(CreateMedicamentCommand command);
}
