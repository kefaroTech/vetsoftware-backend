package com.vetsoftware.app.employee.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteEmployeeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('employee.delete')")
    void execute(Long id);
}
