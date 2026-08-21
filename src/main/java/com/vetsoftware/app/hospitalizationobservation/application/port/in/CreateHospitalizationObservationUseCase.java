package com.vetsoftware.app.hospitalizationobservation.application.port.in;

import com.vetsoftware.app.hospitalizationobservation.application.command.CreateHospitalizationObservationCommand;
import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateHospitalizationObservationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.create') and @authz.isMyCompany(#command.companyId))")
    HospitalizationObservationDto execute(CreateHospitalizationObservationCommand command);
}
