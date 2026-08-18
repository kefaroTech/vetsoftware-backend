package com.vetsoftware.app.hospitalizationmedication.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteHospitalizationMedicationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.delete')"
            + " and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
