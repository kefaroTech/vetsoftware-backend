package com.vetsoftware.app.surgerytype.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteSurgeryTypeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('surgery.delete')"
            + " and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
