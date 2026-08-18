package com.vetsoftware.app.hospitalizationobservation.application.port.in;

import com.vetsoftware.app.hospitalizationobservation.application.command.UpdateHospitalizationObservationCommand;
import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateHospitalizationObservationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.update')"
            + " and @authz.isMyCompany(#command.companyId))")
    HospitalizationObservationDto execute(UpdateHospitalizationObservationCommand command);
}
