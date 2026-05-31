package com.vetsoftware.app.hospitalizationmedication.application.port.in;

import com.vetsoftware.app.hospitalizationmedication.application.command.UpdateHospitalizationMedicationCommand;
import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateHospitalizationMedicationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalizationMedication.update') or hasRole('SYSTEM')")
    HospitalizationMedicationDto execute(UpdateHospitalizationMedicationCommand command);
}
