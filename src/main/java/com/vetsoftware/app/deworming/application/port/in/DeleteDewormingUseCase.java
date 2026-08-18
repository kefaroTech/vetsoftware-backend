package com.vetsoftware.app.deworming.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteDewormingUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('deworming.delete')"
            + " and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
