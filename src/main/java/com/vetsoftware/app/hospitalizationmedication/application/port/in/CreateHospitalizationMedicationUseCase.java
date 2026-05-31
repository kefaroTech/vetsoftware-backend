package com.vetsoftware.app.hospitalizationmedication.application.port.in;

import com.vetsoftware.app.hospitalizationmedication.application.command.CreateHospitalizationMedicationCommand;
import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateHospitalizationMedicationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalization.create') or hasRole('SYSTEM')")
    HospitalizationMedicationDto execute(CreateHospitalizationMedicationCommand command);
}
