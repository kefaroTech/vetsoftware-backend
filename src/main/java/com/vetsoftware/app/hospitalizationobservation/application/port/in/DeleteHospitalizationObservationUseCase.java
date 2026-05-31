package com.vetsoftware.app.hospitalizationobservation.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteHospitalizationObservationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalization.delete') or hasRole('SYSTEM')")
    void execute(Long id);
}
