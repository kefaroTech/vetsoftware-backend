package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateEmployeeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('employee.update')")
    EmployeeDto execute(Long id);
}
