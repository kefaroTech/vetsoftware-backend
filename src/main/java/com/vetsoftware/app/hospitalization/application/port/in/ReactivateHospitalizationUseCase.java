package com.vetsoftware.app.hospitalization.application.port.in;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateHospitalizationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.update')"
            + " and @authz.isMyCompany(#companyId))")
    HospitalizationDto execute(Long id, Long companyId);
}
