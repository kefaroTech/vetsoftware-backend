package com.vetsoftware.app.hospitalizationmedication.application.port.in;

import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateHospitalizationMedicationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.update')"
            + " and @authz.isMyCompany(#companyId))")
    HospitalizationMedicationDto execute(Long id, Long companyId);
}
