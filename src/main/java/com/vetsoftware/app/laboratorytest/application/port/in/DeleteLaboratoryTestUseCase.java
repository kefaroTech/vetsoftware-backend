package com.vetsoftware.app.laboratorytest.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteLaboratoryTestUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('laboratoryTest.delete') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
