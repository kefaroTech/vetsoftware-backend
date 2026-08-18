package com.vetsoftware.app.employee.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteEmployeeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('employee.delete') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
