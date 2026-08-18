package com.vetsoftware.app.employeerole.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteEmployeeRoleUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('employee.delete')"
            + " and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
