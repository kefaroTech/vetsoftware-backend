package com.vetsoftware.app.medicament.application.port.in;

import com.vetsoftware.app.medicament.application.command.CreateMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateMedicamentUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('prescription.create') or hasRole('SYSTEM')")
    MedicamentDto execute(CreateMedicamentCommand command);
}
