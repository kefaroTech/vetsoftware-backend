package com.vetsoftware.app.hospitalizationobservation.application.port.in;

import com.vetsoftware.app.hospitalizationobservation.application.command.UpdateHospitalizationObservationCommand;
import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateHospitalizationObservationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalizationObservation.update') or hasRole('SYSTEM')")
    HospitalizationObservationDto execute(UpdateHospitalizationObservationCommand command);
}
