package com.vetsoftware.app.hospitalizationmedication.application.port.in;

import com.vetsoftware.app.hospitalizationmedication.application.command.UpdateHospitalizationMedicationCommand;
import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateHospitalizationMedicationUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.update')")
  HospitalizationMedicationDto execute(UpdateHospitalizationMedicationCommand command);
}
