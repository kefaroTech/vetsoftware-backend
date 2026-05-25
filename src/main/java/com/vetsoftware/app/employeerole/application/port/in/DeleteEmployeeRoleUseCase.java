package com.vetsoftware.app.employeerole.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteEmployeeRoleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('employee.delete') or hasRole('SYSTEM')")
    void execute(Long id);
}
