package com.vetsoftware.app.hospitalizationmedication.application.port.in;

import com.vetsoftware.app.hospitalizationmedication.application.command.SuspendHospitalizationMedicationCommand;
import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SuspendHospitalizationMedicationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalization.update') or hasRole('SYSTEM')")
    HospitalizationMedicationDto execute(SuspendHospitalizationMedicationCommand command);
}
