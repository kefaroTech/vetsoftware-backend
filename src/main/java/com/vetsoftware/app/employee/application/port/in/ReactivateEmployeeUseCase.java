package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateEmployeeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('employee.update') or hasRole('SYSTEM')")
    EmployeeDto execute(Long id);
}
