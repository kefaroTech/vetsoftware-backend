package com.vetsoftware.app.hospitalizationobservation.application.port.in;

import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateHospitalizationObservationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalization.update') or hasRole('SYSTEM')")
    HospitalizationObservationDto execute(Long id);
}
