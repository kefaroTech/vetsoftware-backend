package com.vetsoftware.app.vaccination.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteVaccinationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('vaccination.delete')"
            + " and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
