package com.vetsoftware.app.employee.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteEmployeeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('employee.delete') or hasRole('SYSTEM')")
    void execute(Long id);
}
