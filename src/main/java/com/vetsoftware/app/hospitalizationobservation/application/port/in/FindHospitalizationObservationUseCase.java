package com.vetsoftware.app.hospitalizationobservation.application.port.in;

import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindHospitalizationObservationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or ((hasAuthority('admin.all') or hasAuthority('hospitalization.read')) and @authz.isMyCompany(#companyId))")
    HospitalizationObservationDto findById(Long id, Long companyId);
}
