package com.vetsoftware.app.prescription.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeletePrescriptionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('prescription.delete') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
