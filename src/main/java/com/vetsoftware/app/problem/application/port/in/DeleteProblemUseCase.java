package com.vetsoftware.app.problem.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteProblemUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('animal.create') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
