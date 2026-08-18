package com.vetsoftware.app.hospitalizationobservation.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteHospitalizationObservationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.delete')"
            + " and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
