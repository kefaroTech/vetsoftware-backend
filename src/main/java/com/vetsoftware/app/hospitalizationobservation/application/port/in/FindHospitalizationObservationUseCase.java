package com.vetsoftware.app.hospitalizationobservation.application.port.in;

import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindHospitalizationObservationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalizationObservation.read') or hasRole('SYSTEM')")
    HospitalizationObservationDto findById(Long id);
}
