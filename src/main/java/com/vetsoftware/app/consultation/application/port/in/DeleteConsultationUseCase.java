package com.vetsoftware.app.consultation.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteConsultationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('consultation.delete') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
