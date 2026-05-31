package com.vetsoftware.app.hospitalizationobservation.application.port.in;

import com.vetsoftware.app.hospitalizationobservation.application.command.CreateHospitalizationObservationCommand;
import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateHospitalizationObservationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalizationObservation.create') or hasRole('SYSTEM')")
    HospitalizationObservationDto execute(CreateHospitalizationObservationCommand command);
}
