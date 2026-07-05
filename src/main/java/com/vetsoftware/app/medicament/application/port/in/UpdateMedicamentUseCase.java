package com.vetsoftware.app.medicament.application.port.in;

import com.vetsoftware.app.medicament.application.command.UpdateMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateMedicamentUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('prescription.update') or hasRole('SYSTEM')")
    MedicamentDto execute(UpdateMedicamentCommand command);
}
