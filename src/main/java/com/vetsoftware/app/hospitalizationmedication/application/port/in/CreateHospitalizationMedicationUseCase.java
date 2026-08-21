package com.vetsoftware.app.hospitalizationmedication.application.port.in;

import com.vetsoftware.app.hospitalizationmedication.application.command.CreateHospitalizationMedicationCommand;
import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateHospitalizationMedicationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.create') and @authz.isMyCompany(#command.companyId))")
    HospitalizationMedicationDto execute(CreateHospitalizationMedicationCommand command);
}
