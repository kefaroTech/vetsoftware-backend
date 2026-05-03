package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindEmployeeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('employee.read') or hasRole('SYSTEM')")
    EmployeeDto findById(Long id);
}
