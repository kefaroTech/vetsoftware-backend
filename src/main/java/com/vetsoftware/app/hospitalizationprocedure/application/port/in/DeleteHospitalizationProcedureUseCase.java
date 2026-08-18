package com.vetsoftware.app.hospitalizationprocedure.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteHospitalizationProcedureUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.delete')"
            + " and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
