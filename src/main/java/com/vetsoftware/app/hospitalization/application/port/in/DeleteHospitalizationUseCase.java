package com.vetsoftware.app.hospitalization.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteHospitalizationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.delete')"
            + " and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
