package com.vetsoftware.app.animalalert.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteAnimalAlertUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('animal.create') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
